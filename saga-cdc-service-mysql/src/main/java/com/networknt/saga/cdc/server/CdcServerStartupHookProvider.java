package com.networknt.saga.cdc.server;

import com.networknt.config.Config;
import com.networknt.eventuate.cdc.common.CdcConfig;

import com.networknt.eventuate.cdc.mysql.*;

import com.networknt.eventuate.kafka.KafkaConfig;
import com.networknt.eventuate.kafka.producer.EventuateKafkaProducer;
import com.networknt.saga.cdc.mysql.MessageWithDestination;
import com.networknt.saga.cdc.mysql.MessageWithDestinationPublishingStrategy;
import com.networknt.saga.cdc.mysql.WriteRowsEventDataParser;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * CdcServer StartupHookProvider. start cdc service
 */
public class CdcServerStartupHookProvider implements StartupHookProvider {
    static String CDC_CONFIG_NAME = "cdc";
    static CdcConfig cdcConfig = (CdcConfig) Config.getInstance().getJsonObjectConfig(CDC_CONFIG_NAME, CdcConfig.class);
    static String KAFKA_CONFIG_NAME = "kafka";
    static KafkaConfig kafkaConfig = (KafkaConfig) Config.getInstance().getJsonObjectConfig(KAFKA_CONFIG_NAME, KafkaConfig.class);

    static HikariDataSource dataSource;

    static {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(cdcConfig.getJdbcUrl());
        dataSource.setUsername(cdcConfig.getDbUser());
        dataSource.setPassword(cdcConfig.getDbPass());
        dataSource.setMaximumPoolSize(cdcConfig.getMaximumPoolSize());
    }

    public static CuratorFramework curatorFramework;
    public static EventTableChangesToAggregateTopicTranslator<MessageWithDestination> translator;

    public void onStartup() {
        CdcStartupValidator cdcStartupValidator = new CdcStartupValidator(cdcConfig.getJdbcUrl(), cdcConfig.getDbUser(), cdcConfig.getDbPass(), cdcConfig.getKafka());
        cdcStartupValidator.validateEnvironment();

        curatorFramework = makeStartedCuratorClient(cdcConfig.getZookeeper());

        SourceTableNameSupplier supplier = new SourceTableNameSupplier(cdcConfig.getSourceTableName(), "MESSAGE");
        IWriteRowsEventDataParser eventDataParser = new WriteRowsEventDataParser(dataSource, supplier.getSourceTableName());
        MySqlBinaryLogClient<MessageWithDestination> mySqlBinaryLogClient = new MySqlBinaryLogClient<>(
                eventDataParser,
                cdcConfig.getDbUser(),
                cdcConfig.getDbPass(),
                cdcConfig.getDbHost(),
                cdcConfig.getDbPort(),
                cdcConfig.getBinlogClientId(),
                supplier.getSourceTableName());


        EventuateKafkaProducer eventuateKafkaProducer = new EventuateKafkaProducer(kafkaConfig.getBootstrapServers());

        DatabaseBinlogOffsetKafkaStore binlogOffsetKafkaStore = new DatabaseBinlogOffsetKafkaStore(
                cdcConfig.getDbHistoryTopicName(), mySqlBinaryLogClient.getName(), eventuateKafkaProducer);

        MySQLCdcProcessor<MessageWithDestination> mySQLCdcProcessor = new MySQLCdcProcessor<>(mySqlBinaryLogClient, binlogOffsetKafkaStore);

        MySQLCdcKafkaPublisher<MessageWithDestination> mySQLCdcKafkaPublisher = new MySQLCdcKafkaPublisher<>(binlogOffsetKafkaStore, kafkaConfig.getBootstrapServers(), new MessageWithDestinationPublishingStrategy());
        translator = new EventTableChangesToAggregateTopicTranslator<>(mySQLCdcKafkaPublisher, mySQLCdcProcessor, curatorFramework, cdcConfig );
        translator.start();

        System.out.println("CdcServerStartupHookProvider is called");
    }

    CuratorFramework makeStartedCuratorClient(String connectionString) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(2000, 6, 2000);
        CuratorFramework client = CuratorFrameworkFactory.
                builder().connectString(connectionString)
                .retryPolicy(retryPolicy)
                .build();
        client.start();
        return client;
    }


}
