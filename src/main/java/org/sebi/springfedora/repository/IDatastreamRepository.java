package org.sebi.springfedora.repository;

import org.sebi.springfedora.model.Datastream;
import org.springframework.data.repository.CrudRepository;

public interface IDatastreamRepository extends CrudRepository<Datastream, String> {
  
  

}
