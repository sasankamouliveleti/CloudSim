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

/*Simulation 1 results out power consumed, cpu utilization metrics and 
summary of simulation results using default VM Allocation Policy which is round robin*/
object Simulation1 {
  val logger: Logger = CreateLogger(classOf[Simulation1]) /* Define the logger*/
  /* Intiate the config parameters for number of hosts, cloudlets, vms etc.*/
  val config: Config = ConfigFactory.load("simulation1.conf").getConfig("simulation1")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")
  def main(args: Array[String]): Unit = {
    executeSimulation() /* main method where all the simulation execution takes place*/
  }
  
  def executeSimulation(): Unit = {
    logger.info("**************Entering Simulation1 ********************")
    val simulation = new CloudSim() /* Intiate simulation*/
    val hostList: List[Host] = InfraHelper.createPowerHostList(config) /* define the hosts */
    val vmsList: List[Vm] = InfraHelper.createVmsList(config) /* define the vms */
    val cloudletList: List[Cloudlet] = InfraHelper.createCloudlets(config) /* define the cloudlets*/
    val dataCenter = new DatacenterSimple(simulation, hostList.asJava, InfraHelper.getTypeOfAllocation(config)) /* Intiate the datacenter*/
    val schedulingInterval = config.getInt("SCHEDULING_INTERVAL") 
    dataCenter.setSchedulingInterval(schedulingInterval)

    val broker = new DatacenterBrokerSimple(simulation) /* Intitate the broker*/

    val networkTopology = new BriteNetworkTopology() /* Intitalise the network topology to be used*/
    simulation.setNetworkTopology(networkTopology)
    networkTopology.addLink(dataCenter, broker, mainConfig.getDouble("NETWORK_BW"), mainConfig.getDouble("NETWORK_LATENCY"))

    broker.submitVmList(vmsList.asJava) /* submit the vms to be create*/
    broker.submitCloudletList(cloudletList.asJava) /* submit the cloudlets*/

    simulation.start()

    val finishedCloudlets = broker.getCloudletFinishedList() /* Get all the finished cloudlets*/
    finishedCloudlets.sort(Comparator.comparingLong((cloudlet: Cloudlet) => cloudlet.getVm.getId))
    
    /* Print summary of results of simulation*/
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
      /*VM CPU utilization relative to the host capacity*/
      val vmRelativeCpuUtilization = vm.getCpuUtilizationStats.getMean / vm.getHost.getVmCreatedList.size
      val vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm // W
      val cpuStats = vm.getCpuUtilizationStats
      
      logger.info(vm.getId.toString + "|" + (cpuStats.getMean * 100).ceil + "|" + vmPower.ceil)
    })
    logger.info("**************Exiting Simulation1********************")
  }
}

class Simulation1