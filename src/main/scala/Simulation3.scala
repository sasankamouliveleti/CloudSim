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

object Simulation3 {
  val logger: Logger = CreateLogger(classOf[Simulation3])
  val config: Config = ConfigFactory.load("simulation3.conf").getConfig("simulation3")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")

  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def executeSimulation(): Unit = {
    logger.info("**************Entering Simulation3********************")
    val simulation = new CloudSim()
    val hostList: List[Host] = InfraHelper.createPowerHostList(config)
    val vmsList: List[Vm] = InfraHelper.createVmsList(config)
    val cloudletList: List[Cloudlet] = InfraHelper.createCloudlets(config)
    val dataCenter = new DatacenterSimple(simulation, hostList.asJava, InfraHelper.getTypeOfAllocation(config))
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

    logger.info("**************Exiting Simulation3********************")
  }
}

class Simulation3