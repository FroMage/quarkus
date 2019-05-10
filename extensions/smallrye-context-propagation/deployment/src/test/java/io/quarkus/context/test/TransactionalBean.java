package io.quarkus.context.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

public class TransactionalBean {

    @Inject
    TransactionManager tm;

    @Transactional
    public void doInTx() {
        try {
            System.err.println("service bean TX: " + tm.getTransaction());
        } catch (SystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
