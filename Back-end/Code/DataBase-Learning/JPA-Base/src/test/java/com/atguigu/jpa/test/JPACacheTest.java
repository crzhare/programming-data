package com.atguigu.jpa.test;

import com.atguigu.jpa.entities.twoway.many2one.Customer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

/**
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2020/4/11 22:53
 */
public class JPACacheTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private EntityTransaction transaction;

    @Before
    public void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("jpa-base");
        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
        transaction.begin();
    }

    @After
    public void destroy() {
        transaction.commit();
        entityManager.close();
        entityManagerFactory.close();
    }

    @Test
    public void testSecondLevelCache(){
        //只会执行一条语句，因为配置了二级缓存。
        Customer customer1 = entityManager.find(Customer.class, 16);
        transaction.commit();
        entityManager.close();

        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
        transaction.begin();
        Customer customer2 = entityManager.find(Customer.class, 16);
    }

    @Test
    public void testFirstLevelCache(){
        //只会执行一条语句，因为存在一级缓存。
        Customer customer1 = entityManager.find(Customer.class, 16);
        Customer customer2 = entityManager.find(Customer.class, 16);
    }

}