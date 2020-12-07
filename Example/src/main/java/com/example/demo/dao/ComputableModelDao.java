package com.example.demo.dao;

import com.example.demo.domain.ComputableModel;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by wang ming on 2019/2/26.
 */
public interface ComputableModelDao extends MongoRepository<ComputableModel,String> {

}
