package org.sebi.springfedora.repository.utils;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.fcrepo.client.DeleteBuilder;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.PostBuilder;
import org.fcrepo.client.PutBuilder;
import org.sebi.springfedora.Common;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FedroaPlatformTransactionManager extends AbstractPlatformTransactionManager {

  @Value("${gams.curHost}")
  private String curHost;

  @Value("${gams.fedoraRESTEndpoint}")
  private String fedoraRESTEndpoint;

  private final String FEDORA_TRANSACTION_ENDPOINT = "/fcr:tx";

  private String txid;

  public FedroaPlatformTransactionManager(){
    log.info("Succefully started custom FedoraPlatformTransactionManager");
  }

  @Override
  protected String doGetTransaction() throws TransactionException {

    // if(txid == null){
    //   throw new TransactionSystemException("Transaction is set to null !");
    // }

    return txid;
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {

    FcrepoClient client = new FcrepoClient.FcrepoClientBuilder().build();

    String location = null;

    try {
      URI uri = URI.create(curHost + fedoraRESTEndpoint + FEDORA_TRANSACTION_ENDPOINT);
      FcrepoResponse response = new PostBuilder(uri, client).perform();
      // TODO location is a bit misleading? - substringAfter ensures that only the
      // txid is being returned.
      location = StringUtils.substringAfter(response.getLocation().getPath(), FEDORA_TRANSACTION_ENDPOINT + "/");
      log.info("Fedora transaction creation status code {} and location: {}", response.getStatusCode(),
          response.getLocation().toString());
      this.txid = location;
      log.info("POST: Succesfully created new fedora transaction with txid: {} - from endpoint: {}", txid, FEDORA_TRANSACTION_ENDPOINT);
      
    } catch (NullPointerException | IllegalArgumentException e) {
      String msg = String.format("Failed at preprocessing for requesting txid from fedora against %s", FEDORA_TRANSACTION_ENDPOINT);
      this.txid = null;
      log.error(msg + "\n" + e);
      new TransactionSystemException(msg);
    } catch(FcrepoOperationFailedException e2){
      this.txid = null;
      String msg = String.format("Failed at creation of new fedora transaction via POST against %s", FEDORA_TRANSACTION_ENDPOINT);
      log.error(msg + "\n" + e2);
      throw new TransactionSystemException(msg);
    }

  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) throws TransactionException {

    FcrepoClient client = new FcrepoClient.FcrepoClientBuilder().build();

    log.debug("Transaction commit location: {}", FEDORA_TRANSACTION_ENDPOINT);
    try {
      if (txid != null && txid.startsWith(Common.TX_PREFIX)) {
        URI uri = URI.create(FEDORA_TRANSACTION_ENDPOINT);
        FcrepoResponse response = new PutBuilder(uri, client).perform();
        log.debug("Transaction commit status and location: {} {}", response.getStatusCode(), uri.toString());
      }
      // TODO log else?
    } catch (Exception e) {
      log.error(e.getMessage());
      // TOO improve!
      throw new TransactionSuspensionNotSupportedException("Transaction failed at doCommit" + e);
    }

  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
    
    FcrepoClient client = new FcrepoClient.FcrepoClientBuilder().build();

    log.debug("Transaction rollback location: {}", FEDORA_TRANSACTION_ENDPOINT);	 
		try {
	    	if (txid != null && txid.startsWith(Common.TX_PREFIX)) {
	    		URI uri = URI.create(FEDORA_TRANSACTION_ENDPOINT);
	    		FcrepoResponse response = new DeleteBuilder(uri, client).perform();
	    		log.debug("Transaction rollback status and location: {} {}", response.getStatusCode(), uri.toString());
	    	}
	    	//TODO log else?
	    } catch (Exception e) {
	    	log.error(e.getMessage());
	    } 

  }

}
