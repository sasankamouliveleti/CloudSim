import HelperUtils.{CreateLogger, InfraHelper}
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.Cloudlet
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudsimplus.builders.tables.{CloudletsTableBuilder, TextTableColumn}
import org.slf4j.Logger

import java.util.Comparator
import scala.jdk.CollectionConverters.*
import java.util.ArrayList

object Simulation3 {
  val logger: Logger = CreateLogger(classOf[Simulation3])
  val config: Config = ConfigFactory.load("simulation3.conf").getConfig("simulation3")
  private val HOSTS: Int = config.getInt("HOSTS")
  private val HOST_PES: Int = config.getInt("HOST_PES")

  private val VMS: Int = config.getInt("VMS")
  private val VM_PES: Int = config.getInt("VM_PES")

  private val CLOUDLETS: Int = config.getInt("CLOUDLETS")
  private val CLOUDLET_PES: Int = config.getInt("CLOUDLET_PES")
  private val CLOUDLET_LENGTH: Int = config.getInt("CLOUDLET_LENGTH")
  private val HOST_START_UP_DELAY = 5

  private val HOST_SHUT_DOWN_DELAY = 3

  private val HOST_START_UP_POWER = 5

  private val HOST_SHUT_DOWN_POWER = 3
  private val STATIC_POWER: Int = 35
  private val MAX_POWER: Int = 50
  private val SCHEDULING_INTERVAL = 10
  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def executeSimulation(): Unit = {
    val simulation = new CloudSim()
    val hostList = new ArrayList[Host](HOSTS)

    def createPowerHost(id: Int): Host = {
      val peList = new ArrayList[Pe](HOST_PES)
      //List of Host's CPUs (Processing Elements, PEs)
      (0 to HOST_PES - 1).map((index) => {
        peList.add(new PeSimple(1000))
      })
      val ram = 2048 //in Megabytes
      val bw = 10000 //in Megabits/s
      val storage = 1000000
      val vmScheduler = new VmSchedulerTimeShared()
      val host = new HostSimple(ram, bw, storage, peList)
      val powerModel = new PowerModelHostSimple(MAX_POWER, STATIC_POWER)
      powerModel.setStartupDelay(HOST_START_UP_DELAY).setShutDownDelay(HOST_SHUT_DOWN_DELAY).setStartupPower(HOST_START_UP_POWER).setShutDownPower(HOST_SHUT_DOWN_POWER)
      host.setVmScheduler(vmScheduler).setPowerModel(powerModel)
      host.setId(id)
      host.enableUtilizationStats()
      host
    }

    def createDatacenter(): Datacenter = {
      (0 to HOSTS - 1).map((index) => {
        val host = createPowerHost(index)
        hostList.add(host)
      })

      val dc = new DatacenterSimple(simulation, hostList)
      dc.setSchedulingInterval(SCHEDULING_INTERVAL)
      dc
    }
    val datacenter0 = createDatacenter()
    val vmList = InfraHelper.createVms(VMS, 1000,VM_PES, 512, 1000, 10000)
    val cloudletList = InfraHelper.createCloudlets(1000, 1000, CLOUDLET_LENGTH, CLOUDLETS, CLOUDLET_PES)
    val broker = new DatacenterBrokerSimple(simulation)
    broker.submitVmList(vmList.asJava)
    broker.submitCloudletList(cloudletList.asJava)

    simulation.start()

    val finishedCloudlets = broker.getCloudletFinishedList()

    val resourceUsageTable = new CloudletsTableBuilder(finishedCloudlets)
      .addColumn(new TextTableColumn("CPU Usage", "seconds"),  cloudlet => "%.2f".format(cloudlet.getActualCpuTime))
      .addColumn(new TextTableColumn("RAM Usage", "Mb"),  cloudlet => "%.2f".format(cloudlet.getUtilizationOfRam))
      .addColumn(new TextTableColumn("Bandwidth", "Mb"),  cloudlet => "%.2f".format(cloudlet.getUtilizationOfBw))

    resourceUsageTable.build()
  }
}

class Simulation3
