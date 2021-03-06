package cn.andyoung.rabbitmq.demo;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

public class ConfirmProducer {

  public static void main(String[] args) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setVirtualHost("/");
    factory.setUsername("guest");
    factory.setPassword("guest");

    Connection connection = factory.newConnection();

    Channel channel = connection.createChannel();

    String exchangeName = "test_confirm_exchange";
    String routingKey = "item.update";

    // 指定消息的投递模式：confirm 确认模式
    channel.confirmSelect();

    // 发送
    final long start = System.currentTimeMillis();
    for (int i = 0; i < 5; i++) {
      String msg = "this is confirm msg ";
      channel.basicPublish(exchangeName, routingKey, null, msg.getBytes());
      System.out.println("Send message : " + msg);
    }

    // 添加一个确认监听， 这里就不关闭连接了，为了能保证能收到监听消息
    channel.addConfirmListener(
        new ConfirmListener() {
          /** 返回成功的回调函数 */
          public void handleAck(long deliveryTag, boolean multiple) throws IOException {
            System.out.println("succuss ack");
            System.out.println(multiple);
            System.out.println("耗时：" + (System.currentTimeMillis() - start) + "ms");
          }
          /** 返回失败的回调函数 */
          public void handleNack(long deliveryTag, boolean multiple) throws IOException {
            System.out.printf("defeat ack");
            System.out.println("耗时：" + (System.currentTimeMillis() - start) + "ms");
          }
        });
  }
}
