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
import org.cloudsimplus.autoscaling.HorizontalVmScalingSimple
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import scala.collection.immutable.List
import scala.util.Random
import java.util
import java.util.Comparator.comparingLong
import java.util.function.Supplier
import java.util.{ArrayList, Comparator}
import scala.jdk.CollectionConverters.*

/* Simulation2 results out the cost of the operation while using the horizontal scalable vms and summary of simulation results using default
VM Allocation Policy*/
object Simulation2 {
  val logger: Logger = CreateLogger(classOf[Simulation2]) /* Define the logger*/
  /* Intiate the config parameters for number of hosts, cloudlets, vms etc.*/
  val config: Config = ConfigFactory.load("simulation2.conf").getConfig("simulation2")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")

  def main(args: Array[String]): Unit = {
    executeSimulation() /* main method where all the simulation execution takes place*/
  }

  def isVmOverloaded(vm: Vm) = vm.getCpuPercentUtilization > mainConfig.getDouble("OVERLOADTHRESH")

  /* Method to create horizontal scalable vms*/
  def createScalableVmsList(): List[Vm] = {
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
        .setCloudletScheduler(InfraHelper.getCloudletSchedularType(vmConfigVal.getString("CLOUDLET_SCHEDULER")))
      val supplierVM: Supplier[Vm] = new Supplier[Vm] {
        override def get(): Vm = new VmSimple(index, 1000, vmConfigVal.getInt("VM_PES"))
          .setRam(vmConfigVal.getInt("RAM"))
          .setBw(vmConfigVal.getInt("BDW"))
          .setSize(vmConfigVal.getInt("SIZE"))
          .setCloudletScheduler(InfraHelper.getCloudletSchedularType(vmConfigVal.getString("CLOUDLET_SCHEDULER")))
      }
      val horizontalScaling = new HorizontalVmScalingSimple()
      horizontalScaling.setVmSupplier(supplierVM).setOverloadPredicate(isVmOverloaded)
      vm.setHorizontalScaling(horizontalScaling)
      vm
    }).toList
    vms
  }

  def executeSimulation(): Unit = {
    logger.info("**************Entering Simulation2 ********************")
    val simulation = new CloudSim() /* Intiate simulation*/
    val hostList: List[Host] = InfraHelper.createHostList(config) /* define the hosts */
    val vmsList: List[Vm] = createScalableVmsList() /* define the vms */
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
    /* Print summary of results of simulation and costs*/
    new CloudletsTableBuilder(finishedCloudlets).build()

    val finshedVms: List[VmSimple] = broker.getVmCreatedList.asScala.toList
    logger.info("The finished vms size is " + finshedVms.length)
    val cost = cloudletList.map((cloudletVal) => {
      if (cloudletVal.isFinished) {
        mainConfig.getDouble("UsagePerSecond") * cloudletVal.getActualCpuTime() * mainConfig.getDouble("BandwidthCost")
      } else {
        0
      }
    }).sum
    logger.info(s"The total cost is ${cost}")

    logger.info("**************Exiting Simulation2********************")
  }
}

class Simulation2