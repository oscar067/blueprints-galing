package mail

import java.util.UUID

import io.gatling.core.Predef.Simulation
import org.apache.kafka.clients.producer.ProducerConfig

import scala.concurrent.duration._
import de.codecentric.gatling.jdbc.Predef._
import io.gatling.core.Predef._
import ru.tinkoff.gatling.kafka.protocol.KafkaProtocol
import com.sksamuel.avro4s._
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.gatling.core.Predef._
import ocp.bluprints.echo.Event
import org.apache.kafka.clients.producer
import org.apache.kafka.clients.producer.ProducerConfig
import ru.tinkoff.gatling.kafka.Predef._
import ru.tinkoff.gatling.kafka.protocol.KafkaProtocol


class MailLoadTest extends Simulation{

  val jdbcConfig = jdbc.url("jdbc:postgresql://localhost:5432/postgres").username("postgres").password("postgres").driver("org.postgresql.Driver")

  val feeder = Iterator.continually(Map("theEvent" -> getPayLoad(UUID.randomUUID().toString)))



  val kafkaConf: KafkaProtocol = kafka
    // Kafka topic name
    .topic("demoGatlingTopic")
    // Kafka producer configs
    .properties(
      Map(
        ProducerConfig.ACKS_CONFIG -> "1",
        // list of Kafka broker hostname and port pairs
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:29092",
        // in most cases, StringSerializer or ByteArraySerializer
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG ->
          "org.apache.kafka.common.serialization.StringSerializer",
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG ->
          "io.confluent.kafka.serializers.KafkaAvroSerializer",
        "schema.registry.url" -> "http://0.0.0.0:8081"

      ))

  val scnKafka = scenario("Kafka Test").feed(feeder).
    exec(
        kafka("request")
          // message to send
          .send[Event]("${theEvent}"))



  val scnJdbc =  scenario("selectRecords").exec(jdbc("selection")
    .select("select c.count ")
    .from("select count (traceid)  from gatlingpoccheck) as c ").where("c.count = 10000"))

  def getPayLoad(traceId:String): Event = {
    System.err.println(traceId)
    val theEvent =  new ocp.bluprints.echo.Event();
    theEvent.setLdd("ING_CZ")
    theEvent.setId(traceId)
    theEvent.setTraceId(traceId)
    theEvent.setCreated(String.valueOf(System.currentTimeMillis()))
    theEvent.setPersonId(traceId)
    theEvent.setFilter("firstFilter")
    theEvent.setVersion("1.0")
    theEvent.setConfidentiality("5")
    theEvent.setSource("gatling")
    theEvent
  }


  setUp(
    scnKafka
      .inject(constantUsersPerSec(10) during(90 seconds)),
    scnJdbc.inject(nothingFor(120 seconds),atOnceUsers(1))

  ).protocols(kafkaConf,jdbcConfig).assertions(details("  selection").failedRequests.count.is(0))



}

