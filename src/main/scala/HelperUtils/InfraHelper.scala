package HelperUtils

import HelperUtils.{CreateLogger, InfraHelper}
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
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import scala.collection.immutable.List
import scala.util.Random
import java.util
import java.util.Comparator.comparingLong
import java.util.{ArrayList, Comparator}
import scala.jdk.CollectionConverters.*

object InfraHelper {
  val logger: Logger = CreateLogger(classOf[InfraHelper])

  def createHostList(config: Config): List[Host] = {
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
      host
    }).toList
    hosts
  }
  def createPowerHostList(config: Config): List[Host] = {
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


  def getTypeOfAllocation(config: Config): VmAllocationPolicy = {
    val alloactionPolicy = config.getString("ALLOCATION_POLICY")
    logger.info("The allocation policy used is" + alloactionPolicy)
    alloactionPolicy match {
      case "ROUND_ROBIN" => new VmAllocationPolicyRoundRobin()
      case "SIMPLE" => new VmAllocationPolicySimple()
      case "BEST_FIT" => new VmAllocationPolicyBestFit()
      case "FIRST_FIT" => new VmAllocationPolicyFirstFit()
    }
  }

  def getCloudletSchedularType(typeVal: String) = {
    logger.info("The clouletscheduler used is" + typeVal)
    typeVal match {
      case "Time" => new CloudletSchedulerTimeShared()
      case "Space" => new CloudletSchedulerSpaceShared()
    }
  }

  def createVmsList(config: Config): List[Vm] = {
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

  def createCloudlets(config: Config): List[Cloudlet] = {
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
}

class InfraHelper
