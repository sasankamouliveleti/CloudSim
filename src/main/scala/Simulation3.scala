import HelperUtils.CreateLogger
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicyBestFit, VmAllocationPolicyFirstFit, VmAllocationPolicyRoundRobin, VmAllocationPolicySimple}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.network.topologies.BriteNetworkTopology
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletSchedulerSpaceShared, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.utilizationmodels.{UtilizationModelDynamic, UtilizationModelFull}
import org.cloudbus.cloudsim.vms.{Vm, VmSimple}
import org.cloudsimplus.builders.tables.{CloudletsTableBuilder, TextTableColumn}
import org.slf4j.Logger

import scala.collection.immutable.List
import scala.util.Random
import java.util
import java.util.Comparator.comparingLong
import java.util.{ArrayList, Comparator}
import scala.jdk.CollectionConverters.*

object Simulation3 {
  val logger: Logger = CreateLogger(classOf[Simulation1])
  val config: Config = ConfigFactory.load("simulation3.conf").getConfig("Simulation3")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationConfigParams")

  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def createPowerHostList(): List[Host] = {
    val hostConfig = config.getConfigList("HOSTS")
    val numberOfHosts = config.getInt("HOSTS_COUNT")
    logger.info("The number of hosts" + numberOfHosts)
    val hosts = (0 until numberOfHosts).map(index => {
      val typeOfHost = Random.between(0, hostConfig.size())
      val hostConfigVal = hostConfig.get(typeOfHost)
      val HOST_PES = hostConfigVal.getInt("PES")

      logger.info("The type of host" + typeOfHost)
      logger.info("The hosts values:" + hostConfigVal)

      val peList = new util.ArrayList[Pe](hostConfigVal.getInt("PES"))
      (0 to HOST_PES - 1).map(index => {
        peList.add(new PeSimple(hostConfigVal.getInt("MIPS")))
      })
      val ram: Long = hostConfigVal.getInt("RAM")
      val bw: Long = hostConfigVal.getInt("BDW")
      val storage: Long = hostConfigVal.getInt("STORAGE")

      val host = new HostSimple(ram, bw, storage, peList, false)
      hostConfigVal.getString("VM_SCHEDULER") match
        case "Time" => host.setVmScheduler(new VmSchedulerTimeShared())
        case "Space" => host.setVmScheduler(new VmSchedulerSpaceShared())

      val powerModel = new PowerModelHostSimple(config.getInt("MAX_POWER"), config.getInt("STATIC_POWER"))
      powerModel.setStartupDelay(config.getInt("HOST_START_UP_DELAY")).setShutDownDelay(config.getInt("HOST_SHUT_DOWN_DELAY")).setStartupPower(config.getInt("HOST_START_UP_POWER")).setShutDownPower(config.getInt("HOST_SHUT_DOWN_POWER"))
      host.setPowerModel(powerModel)
      host.enableUtilizationStats()
      host
    }).toList
    hosts
  }


  def getTypeOfAllocation(): VmAllocationPolicy = {
    val alloactionPolicy = config.getString("ALLOCATION_POLICY")
    alloactionPolicy match {
      case "ROUND_ROBIN" => new VmAllocationPolicyRoundRobin()
      case "SIMPLE" => new VmAllocationPolicySimple()
      case "BEST_FIT" => new VmAllocationPolicyBestFit()
      case "FIRST_FIT" => new VmAllocationPolicyFirstFit()
    }
  }

  def getCloudletSchedularType(typeVal: String) = {
    typeVal match {
      case "Time" => new CloudletSchedulerTimeShared()
      case "Space" => new CloudletSchedulerSpaceShared()
    }
  }

  def createVmsList(): List[Vm] = {
    val vmsConfig = config.getConfigList("VMS")
    val noOfVms = config.getInt("VMS_COUNT")
    val vms = (0 to noOfVms - 1).map(index => {
      val typeOfVm = Random.between(0, vmsConfig.size())
      val vmConfigVal = vmsConfig.get(typeOfVm)

      logger.info("The type of host" + typeOfVm)
      logger.info("The hosts values:" + vmConfigVal)

      val vm = new VmSimple(index, 1000, vmConfigVal.getInt("VM_PES"))
        .setRam(vmConfigVal.getInt("RAM"))
        .setBw(vmConfigVal.getInt("BDW"))
        .setSize(vmConfigVal.getInt("SIZE"))
        .setCloudletScheduler(getCloudletSchedularType(vmConfigVal.getString("CLOUDLET_SCHEDULER")))
      vm.enableUtilizationStats()
      vm
    }).toList
    vms
  }

  def createCloudlets(): List[Cloudlet] = {
    val cloudletConfig = config.getConfigList("CLOUDLETS")
    val noOfCloudlets = config.getInt("CLOUDLETS_COUNT")
    val cloudlets = (0 to noOfCloudlets - 1).map(index => {
      val typeOfCloudlet = Random.between(0, cloudletConfig.size())
      val cloudletConfigVal = cloudletConfig.get(typeOfCloudlet)
      val utilization: UtilizationModelDynamic = new UtilizationModelDynamic(0.2)
      val cloudlet: Cloudlet = new CloudletSimple(index, cloudletConfigVal.getInt("LENGTH"), cloudletConfigVal.getInt("PES"))
        .setFileSize(cloudletConfigVal.getInt("SIZE"))
        .setUtilizationModelCpu(new UtilizationModelFull).setUtilizationModelRam(utilization).setUtilizationModelBw(utilization)
      cloudlet
    }).toList
    cloudlets
  }
  def executeSimulation(): Unit = {
    val simulation = new CloudSim()
    val hostList: List[Host] = createPowerHostList()
    val vmsList: List[Vm] = createVmsList()
    val cloudletList: List[Cloudlet] = createCloudlets()
    val dataCenter = new DatacenterSimple(simulation, hostList.asJava, getTypeOfAllocation())
    val schedulingInterval = config.getInt("SCHEDULING_INTERVAL")
    dataCenter.setSchedulingInterval(schedulingInterval)

    val broker = new DatacenterBrokerSimple(simulation)

    broker.submitVmList(vmsList.asJava)
    broker.submitCloudletList(cloudletList.asJava)

    simulation.start()

    val finishedCloudlets = broker.getCloudletFinishedList()

    val resourceUsageTable = new CloudletsTableBuilder(finishedCloudlets)
      .addColumn(new TextTableColumn("CPU Usage", "seconds"), cloudlet => "%.2f".format(cloudlet.getActualCpuTime))
      .addColumn(new TextTableColumn("RAM Usage", "Mb"), cloudlet => "%.2f".format(cloudlet.getUtilizationOfRam))
      .addColumn(new TextTableColumn("Bandwidth", "Mb"), cloudlet => "%.2f".format(cloudlet.getUtilizationOfBw))

    resourceUsageTable.build()
  }
}

class Simulation3