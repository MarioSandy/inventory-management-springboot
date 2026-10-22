package com.example.inventory_api_springboot.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange Constants
    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String FILE_ORCHESTRA_EXCHANGE = "file-orchestra.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String SHARED_DLX = "shared.dlx";

    // Queue Constants
    public static final String PRODUCT_BATCH_CREATE_QUEUE = "product.create-batch.queue";
    public static final String INVENTORY_STOCK_OPNAME_QUEUE = "inventory.stock-opname.queue";
    public static final String ORDER_PROCESSING_QUEUE = "order.process.queue";
    public static final String FILE_ORCHESTRA_PRODUCT_BATCH_CREATE_QUEUE = "file-orchestra.product.create-batch.queue";
    public static final String FILE_ORCHESTRA_INVENTORY_STOCK_OPNAME_QUEUE = "file-orchestra.inventory.stock-opname.queue";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String CENTRAL_DLQ = "central.dead.letter.queue";
    public static final String DLQ_ROUTING_KEY = "dead.letter.key";

    // ==========================================
    // 1. Central Dead Letter Infrastructure (DLX & DLQ)
    // ==========================================
    @Bean
    public DirectExchange sharedDlx() {
        return new DirectExchange(SHARED_DLX);
    }

    @Bean
    public Queue centralDlq() {
        return QueueBuilder.durable(CENTRAL_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue centralDlq, DirectExchange sharedDlx) {
        return BindingBuilder.bind(centralDlq).to(sharedDlx).with(DLQ_ROUTING_KEY);
    }

    // ==========================================
    // 2. Business Exchanges
    // ==========================================
    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() { return new TopicExchange(ORDER_EXCHANGE); }

    @Bean
    public TopicExchange fileOrchestraExchange() {
        return new TopicExchange(FILE_ORCHESTRA_EXCHANGE);
    }

    @Bean
    public FanoutExchange notificationExchange() {
        return new FanoutExchange(NOTIFICATION_EXCHANGE);
    }

    // ==========================================
    // 3. Business Queues
    // ==========================================
    @Bean
    public Queue productBatchCreateQueue() {
        return createQueueWithRetry(PRODUCT_BATCH_CREATE_QUEUE);
    }

    @Bean
    public Queue inventoryStockOpnameQueue() {
        return createQueueWithRetry(INVENTORY_STOCK_OPNAME_QUEUE);
    }

    @Bean
    public Queue orderProcessingQueue() { return createQueueWithRetry(ORDER_PROCESSING_QUEUE); }

    @Bean
    public Queue fileOrchestraProductBatchCreateQueue() {
        return createQueueWithRetry(FILE_ORCHESTRA_PRODUCT_BATCH_CREATE_QUEUE);
    }

    @Bean
    public Queue fileOrchestraInventoryStockOpnameQueue() {
        return createQueueWithRetry(FILE_ORCHESTRA_INVENTORY_STOCK_OPNAME_QUEUE);
    }

    @Bean
    public Queue emailQueue() {
        // Keeping email out of the DLX loop as requested in your original code
        return new Queue(EMAIL_QUEUE, true);
    }

    // ==========================================
    // 4. Bindings
    // ==========================================
    @Bean Binding bindFileOrchestraProductBatchCreateQueue(Queue fileOrchestraProductBatchCreateQueue, TopicExchange fileOrchestraExchange) {
        return BindingBuilder.bind(fileOrchestraProductBatchCreateQueue).to(fileOrchestraExchange).with("file-orchestra.product.create-batch");
    }

    @Bean Binding bindFileOrchestraInventoryStockOpnameQueue(Queue fileOrchestraInventoryStockOpnameQueue, TopicExchange fileOrchestraExchange) {
        return BindingBuilder.bind(fileOrchestraInventoryStockOpnameQueue).to(fileOrchestraExchange).with("file-orchestra.inventory.stock-opname");
    }

    @Bean
    public Binding bindProductBatchCreateQueue(Queue productBatchCreateQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productBatchCreateQueue).to(productExchange).with("product.create-batch");
    }

    @Bean
    public Binding bindInventoryStockOpnameQueue(Queue inventoryStockOpnameQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(inventoryStockOpnameQueue).to(inventoryExchange).with("inventory.stock-opname");
    }

    @Bean
    public Binding bindOrderProcessingQueue(Queue orderProcessingQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderProcessingQueue).to(orderExchange).with("order.process");
    }

    // NOTE: Make sure to implement bindings for your fileOrchestraExchange queues here if needed!

    @Bean
    public Binding bindEmailToNotif(Queue emailQueue, FanoutExchange notificationExchange) {
        // FIXED: Parameter name now matches the exact Bean name 'notificationExchange'
        return BindingBuilder.bind(emailQueue).to(notificationExchange);
    }

    // ==========================================
    // 5. Serialization & Infrastructure Templates
    // ==========================================
    @Bean
    public MessageConverter jsonMessageConverter() {
        // FIXED: Replaced legacy JacksonJsonMessageConverter with modern Jackson2 version
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // Helper Method
    private Queue createQueueWithRetry(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", SHARED_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }
}
