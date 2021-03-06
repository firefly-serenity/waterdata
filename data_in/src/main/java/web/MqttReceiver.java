package web;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

public class MqttReceiver implements MqttCallback {
    String tmpl = "{\"temp\":27.25,\"pH\":\"3.703\"}";
    Boolean use_tmpl=false;
    MqttConnectOptions options = new MqttConnectOptions();
    MqttClient client;
    MongoOperations mongoOperation;
    Boolean meas_active=false;
    String meas_name="asdafsd";

    public void mqtt_main() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(SpringMongoConfig.class);
        mongoOperation = (MongoOperations)ctx.getBean("mongoTemplate");
        if(use_tmpl) {
            while(true) {
                handleData(tmpl);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            getMessage();
        }
    }

    public void getMessage() {
        try {
            client = new MqttClient("tcp://eu.thethings.network", "Sending");
            options.setUserName("sus2018_waterdata_mqtt");
            options.setPassword("ttn-account-v2.A1onZwjnWTPBUchbIi65cbdIfkYU4Msvao4wFuUWcRQ".toCharArray());
            client.connect(options);
            client.setCallback(this);
            client.subscribe("sus2018_waterdata_mqtt/devices/waterdata_cata1/up");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("error: connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String decodemess = new JSONObject(message.toString()).getString("payload_raw");
        byte[] decoded = Base64.getMimeDecoder().decode(decodemess);
        String output = new String(decoded);
        System.out.println("Got Data via MQTT");
        handleData(output);
    }
    public void handleData(String output) {
        try {
            getMeasurementInfo();
            if (meas_active == true) {
                JSONObject dp = new JSONObject(output);
                Data _d1 = new Data();
                _d1.setSensor("pH");
                _d1.setValue(Double.parseDouble(dp.get("pH").toString()));
                _d1.setTimestamp();
                _d1.setMeasureID(meas_name);
                Data _d2 = new Data();
                _d2.setSensor("temp");
                _d2.setValue(Double.parseDouble(dp.get("temp").toString()));
                _d2.setTimestamp();
                _d2.setMeasureID(meas_name);
                System.out.println(_d1.toString());
                System.out.println(_d2.toString());
                mongoOperation.save(_d1, "data");
                mongoOperation.save(_d2, "data");
            }
        } catch(Exception e) {

        }
    }
    public void getMeasurementInfo() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            is_running ru = restTemplate.getForObject("http://127.0.0.1:8081/measurement/is_running", is_running.class);
            System.out.println(ru.toString());
            meas_active = ru.getRunning();
            meas_name = ru.getId();
        } catch (Exception e){
            System.out.println("Error getting HTTP");
            throw e;
        }

    }
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }

}
