package com.example.demo.dao;

import com.example.demo.domain.ModelContainer;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by wang ming on 2019/4/22.
 */
public interface ModelContainerDao extends MongoRepository<ModelContainer,String> {

}
