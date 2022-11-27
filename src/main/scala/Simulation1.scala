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

object Simulation1 {
  val logger: Logger = CreateLogger(classOf[Simulation1])
  val config: Config = ConfigFactory.load("simulation1.conf").getConfig("simulation1")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")
  def main(args: Array[String]): Unit = {
    executeSimulation()
  }
  
  def executeSimulation(): Unit = {
    logger.info("**************Entering Simulation1 ********************")
    val simulation = new CloudSim()
    val hostList: List[Host] = InfraHelper.createPowerHostList(config)
    val vmsList: List[Vm] = InfraHelper.createVmsList(config)
    val cloudletList: List[Cloudlet] = InfraHelper.createCloudlets(config)
    val dataCenter = new DatacenterSimple(simulation, hostList.asJava, InfraHelper.getTypeOfAllocation(config))
    val schedulingInterval = config.getInt("SCHEDULING_INTERVAL")
    dataCenter.setSchedulingInterval(schedulingInterval)

    val broker = new DatacenterBrokerSimple(simulation)

    val networkTopology = new BriteNetworkTopology()
    simulation.setNetworkTopology(networkTopology)
    networkTopology.addLink(dataCenter, broker, mainConfig.getDouble("NETWORK_BW"), mainConfig.getDouble("NETWORK_LATENCY"))

    broker.submitVmList(vmsList.asJava)
    broker.submitCloudletList(cloudletList.asJava)

    simulation.start()

    val finishedCloudlets = broker.getCloudletFinishedList()
    finishedCloudlets.sort(Comparator.comparingLong((cloudlet: Cloudlet) => cloudlet.getVm.getId))
    new CloudletsTableBuilder(finishedCloudlets).build()


    logger.info(vmsList.asJava.size().toString)
    //vmList.asJava.sort(comparingLong((vm: Vm) => vm.getHost.getId))
    logger.info("VM ID | CPU Mean Usage | Power Consumption Mean")
    vmsList.asJava.forEach((vm) => {
      val powerModel = vm.getHost.getPowerModel
      val hostStaticPower = {
        if (powerModel.isInstanceOf[PowerModelHostSimple]) {
          powerModel.getPower()
        } else {
          0
        }
      }
      val hostStaticPowerByVm = hostStaticPower / vm.getHost.getVmCreatedList.size
      //VM CPU utilization relative to the host capacity
      val vmRelativeCpuUtilization = vm.getCpuUtilizationStats.getMean / vm.getHost.getVmCreatedList.size
      val vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm // W
      val cpuStats = vm.getCpuUtilizationStats
      
      logger.info(vm.getId.toString + "|" + (cpuStats.getMean * 100).ceil + "|" + vmPower.ceil)
    })
    logger.info("**************Exiting Simulation1********************")
  }
}

class Simulation1