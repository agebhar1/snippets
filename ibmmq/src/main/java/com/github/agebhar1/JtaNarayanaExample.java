package com.github.agebhar1;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.ibm.mq.jms.MQXAConnectionFactory;
import com.ibm.msg.client.jms.DetailedJMSException;
import com.ibm.msg.client.wmq.WMQConstants;
import org.postgresql.xa.PGXADataSource;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class JtaNarayanaExample {

    // https://github.com/jbosstm/quickstart/blob/main/ArjunaJTA/object_store/src/main/java/org/jboss/narayana/jta/quickstarts/JDBCStoreExample.java
    // https://www.ibm.com/docs/en/i/7.5?topic=transactions-example-using-jta-handle-transaction
    // https://www.progress.com/tutorials/jdbc/understanding-jta
    // https://access.redhat.com/documentation/en-us/red_hat_fuse/7.2/html/apache_karaf_transaction_guide/using-transaction-manager#xa-enlistment-problem

    public static void setupDatabase(final Connection connection) throws SQLException {
        final Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS Test(data text NOT NULL)");
        stmt.close();
    }

    public static void main(String[] args) throws Exception {

        Locale.setDefault(Locale.US);

        arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier("1");
        final TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

        final PGXADataSource pgXADataSource = new PGXADataSource();
        pgXADataSource.setURL("jdbc:postgresql://localhost/");
        pgXADataSource.setUser("postgres");
        pgXADataSource.setPassword("passw0rd");
        final javax.sql.XAConnection xaJdbcConnection = pgXADataSource.getXAConnection();
        setupDatabase(xaJdbcConnection.getConnection());

        final MQXAConnectionFactory cf = new MQXAConnectionFactory();
        cf.setStringProperty(WMQConstants.WMQ_CONNECTION_NAME_LIST, "localhost(1414)");
        cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        cf.setStringProperty(WMQConstants.WMQ_CHANNEL, "DEV.APP.SVRCONN");
        cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, "QM1");
        cf.setStringProperty(WMQConstants.USERID, "app");
        cf.setStringProperty(WMQConstants.PASSWORD, "passw0rd");

        try (final XAConnection xaJmsConnection = cf.createXAConnection(); final XASession xaSession = xaJmsConnection.createXASession()) {
            final MessageConsumer consumer = xaSession.createConsumer(xaSession.createQueue("DEV.QUEUE.1"));
            final MessageProducer producerOne = xaSession.createProducer(xaSession.createQueue("DEV.QUEUE.2"));
            final MessageProducer producerTwo = xaSession.createProducer(xaSession.createQueue("DEV.QUEUE.3"));

            xaJmsConnection.start();

            final UserTransaction userTransaction = com.arjuna.ats.jta.UserTransaction.userTransaction();
            while (true) {
                userTransaction.begin();
                tm.getTransaction().enlistResource(xaSession.getXAResource());
                tm.getTransaction().enlistResource(xaJdbcConnection.getXAResource());
                try {
                    System.out.println(">> receive");
                    final Message message = consumer.receive(2500);
                    if (message instanceof TextMessage) {
                        final String text = ((TextMessage) message).getText();
                        System.out.println(text);

                        try (final PreparedStatement ps = xaJdbcConnection.getConnection().prepareStatement("INSERT INTO Test(data) VALUES (?)")) {
                            ps.setString(1, text);
                            ps.execute();
                        }
                        producerOne.send(message);
                        producerTwo.send(message);
                    }
                    userTransaction.commit();
                } catch (final DetailedJMSException e) {
                    System.out.format("Exception >> %s%n", e.getExplanation());
                    System.out.format("    cause >> %s%n", e.getLinkedException());
                    userTransaction.rollback();
                    Thread.sleep(5000);
                } catch (final Exception e) {
                    System.out.format("Exception >> %s%n", e);
                    userTransaction.rollback();
                    Thread.sleep(5000);
                }
            }
        }
    }

}
