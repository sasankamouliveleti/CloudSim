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
import org.cloudsimplus.builders.tables.{CloudletsTableBuilder, TextTableColumn}
import org.slf4j.Logger

import scala.collection.immutable.List
import scala.util.Random
import java.util
import java.util.Comparator.comparingLong
import java.util.{ArrayList, Comparator}
import scala.jdk.CollectionConverters.*

/*Results out the RAM, CPU usage and different other metrics of VMs*/
object Simulation3 {
  val logger: Logger = CreateLogger(classOf[Simulation3]) /* Define the logger*/
  /* Intiate the config parameters for number of hosts, cloudlets, vms etc.*/
  val config: Config = ConfigFactory.load("simulation3.conf").getConfig("simulation3")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")

  def main(args: Array[String]): Unit = {
    executeSimulation() /* main method where all the simulation execution takes place*/
  }

  def executeSimulation(): Unit = {
    logger.info("**************Entering Simulation3********************")
    val simulation = new CloudSim() /* Intiate simulation*/
    val hostList: List[Host] = InfraHelper.createPowerHostList(config) /* define the hosts */
    val vmsList: List[Vm] = InfraHelper.createVmsList(config) /* define the vms */
    val cloudletList: List[Cloudlet] = InfraHelper.createCloudlets(config) /* define the cloudlets*/
    val dataCenter = new DatacenterSimple(simulation, hostList.asJava, InfraHelper.getTypeOfAllocation(config)) /* Intiate the datacenter*/
    val schedulingInterval = config.getInt("SCHEDULING_INTERVAL")
    dataCenter.setSchedulingInterval(schedulingInterval)

    val broker = new DatacenterBrokerSimple(simulation) /* Intitate the broker*/

    broker.submitVmList(vmsList.asJava) /* submit the vms to be create*/
    broker.submitCloudletList(cloudletList.asJava) /* submit the cloudlets*/

    simulation.start()

    val finishedCloudlets = broker.getCloudletFinishedList()

    val resourceUsageTable = new CloudletsTableBuilder(finishedCloudlets)
      .addColumn(new TextTableColumn("CPU Usage", "seconds"), cloudlet => "%.2f".format(cloudlet.getActualCpuTime))
      .addColumn(new TextTableColumn("RAM Usage", "Mb"), cloudlet => "%.2f".format(cloudlet.getUtilizationOfRam))
      .addColumn(new TextTableColumn("Bandwidth", "Mb"), cloudlet => "%.2f".format(cloudlet.getUtilizationOfBw))
    /* Print summary of results of simulation and costs*/
    resourceUsageTable.build()

    logger.info("**************Exiting Simulation3********************")
  }
}

class Simulation3