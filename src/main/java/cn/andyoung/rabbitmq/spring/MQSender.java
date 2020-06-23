package cn.andyoung.rabbitmq.spring;

import cn.andyoung.rabbitmq.config.MQConfig;
import cn.andyoung.rabbitmq.entiy.User;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class MQSender {

  @Autowired private RabbitTemplate rabbitTemplate;

  final RabbitTemplate.ConfirmCallback confirmCallback =
      new RabbitTemplate.ConfirmCallback() {

        public void confirm(CorrelationData correlationData, boolean ack, String cause) {
          System.out.println("correlationData: " + correlationData);
          System.out.println("ack: " + ack);
          if (!ack) {
            System.out.println("异常处理....");
          }
        }
      };

  final RabbitTemplate.ReturnCallback returnCallback =
      new RabbitTemplate.ReturnCallback() {

        public void returnedMessage(
            Message message, int replyCode, String replyText, String exchange, String routingKey) {
          System.out.println(
              "return exchange: "
                  + exchange
                  + ", routingKey: "
                  + routingKey
                  + ", replyCode: "
                  + replyCode
                  + ", replyText: "
                  + replyText);
        }
      };

  // 发送消息方法调用: 构建Message消息
  public void send(Object message, Map<String, Object> properties) throws Exception {
    MessageProperties mp = new MessageProperties();
    // 在生产环境中这里不用Message，而是使用 fastJson 等工具将对象转换为 json 格式发送
    Message msg = new Message(message.toString().getBytes(), mp);
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setConfirmCallback(confirmCallback);
    rabbitTemplate.setReturnCallback(returnCallback);
    // id + 时间戳 全局唯一
    CorrelationData correlationData = new CorrelationData("1234567890" + new Date());
    rabbitTemplate.convertAndSend("BOOT-EXCHANGE-1", "boot.save", msg, correlationData);
  }
  // 发送消息方法调用: 构建Message消息
  public void sendUser(User user) throws Exception {
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setConfirmCallback(confirmCallback);
    rabbitTemplate.setReturnCallback(returnCallback);
    // id + 时间戳 全局唯一
    CorrelationData correlationData = new CorrelationData("1234567890" + new Date());
    rabbitTemplate.convertAndSend("BOOT-EXCHANGE-1", "boot.save", user, correlationData);
  }

  // 延迟队列
  public void sendLazy(Object message) {
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setConfirmCallback(confirmCallback);
    rabbitTemplate.setReturnCallback(returnCallback);
    // id + 时间戳 全局唯一
    CorrelationData correlationData = new CorrelationData("12345678909" + new Date());

    System.out.println("SEND MESSAGE" + new Date());
    // 发送消息时指定 header 延迟时间
    rabbitTemplate.convertAndSend(
        MQConfig.LAZY_EXCHANGE,
        "lazy.boot",
        message,
        new MessagePostProcessor() {
          @Override
          public Message postProcessMessage(Message message) throws AmqpException {
            // 设置消息持久化
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            // message.getMessageProperties().setHeader("x-delay", "6000");
            message.getMessageProperties().setDelay(60000);
            return message;
          }
        },
        correlationData);

    try {
      Thread.sleep(100000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
