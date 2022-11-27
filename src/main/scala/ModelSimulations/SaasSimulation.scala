package ModelSimulations

import HelperUtils.CreateLogger
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicyBestFit, VmAllocationPolicyFirstFit, VmAllocationPolicyRoundRobin, VmAllocationPolicySimple}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletSchedulerSpaceShared, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.utilizationmodels.{UtilizationModelDynamic, UtilizationModelFull}
import org.cloudbus.cloudsim.vms.{Vm, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util
import java.util.Comparator
import java.util.function.Consumer
import scala.util.Random
import scala.jdk.CollectionConverters.*


object SaasSimulation {
  val logger: Logger = CreateLogger(classOf[SaasSimulation])
  val config: Config = ConfigFactory.load("saas.conf").getConfig("saas")

  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def createHostList(): List[Host] = {
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
      (0 to HOST_PES - 1).map((index) => {
        peList.add(new PeSimple(hostConfigVal.getInt("MIPS")))
      })
      val ram: Long = hostConfigVal.getInt("RAM")
      val bw: Long = hostConfigVal.getInt("BDW")
      val storage: Long = hostConfigVal.getInt("STORAGE")

      val host = new HostSimple(ram, bw, storage, peList, false)
      hostConfigVal.getString("VM_SCHEDULER") match
        case "Time" => host.setVmScheduler(new VmSchedulerTimeShared())
        case "Space" => host.setVmScheduler(new VmSchedulerSpaceShared())
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
      vm
    }).toList
    vms
  }

  def createCloudlets(): List[Cloudlet] = {
    val cloudletConfig = config.getConfigList("CLOUDLETS")
    val noOfCloudlets = config.getInt("CLOUDLETS_COUNT") + config.getInt("CLOUDLETS_REQUEST_COUNT") 
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
    logger.info("**************Entering SaasSimulation ********************")
    val simulation = new CloudSim()
    val hostList: List[Host] = createHostList()
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
    finishedCloudlets.sort(Comparator.comparingLong((cloudlet: Cloudlet) => cloudlet.getVm.getId))
    new CloudletsTableBuilder(finishedCloudlets).build()

    logger.info("**************Exiting SaasSimulation********************")
  }
}

class SaasSimulation