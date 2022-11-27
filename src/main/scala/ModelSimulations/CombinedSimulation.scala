package ModelSimulations

import HelperUtils.CreateLogger
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.Cloudlet
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.network.topologies.BriteNetworkTopology
import org.cloudbus.cloudsim.vms.Vm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util.Comparator
import scala.collection.immutable.List
import scala.jdk.CollectionConverters.*

object CombinedSimulation {
  val logger: Logger = CreateLogger(classOf[CombinedSimulation])
  val config1: Config = ConfigFactory.load("iaas.conf").getConfig("iaas")
  val config2: Config = ConfigFactory.load("saas.conf").getConfig("saas")
  val config3: Config = ConfigFactory.load("paas.conf").getConfig("paas")
  val mainConfig: Config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")

  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def executeSimulation(): Unit= {
    logger.info("**************Entering CombinedSimulation ********************")
    val simulation = new CloudSim()

    val hostList1: List[Host] = IaasSimulation.createHostList()
    val vmsList1: List[Vm] = IaasSimulation.createVmsList()
    val cloudletList1: List[Cloudlet] = IaasSimulation.createCloudlets()
    val dataCenter1 = new DatacenterSimple(simulation, hostList1.asJava, IaasSimulation.getTypeOfAllocation())
    val schedulingInterval1 = config1.getInt("SCHEDULING_INTERVAL")
    dataCenter1.setSchedulingInterval(schedulingInterval1)

    val hostList2: List[Host] = SaasSimulation.createHostList()
    val vmsList2: List[Vm] = SaasSimulation.createVmsList()
    val cloudletList2: List[Cloudlet] = SaasSimulation.createCloudlets()
    val dataCenter2 = new DatacenterSimple(simulation, hostList2.asJava, SaasSimulation.getTypeOfAllocation())
    val schedulingInterval2 = config2.getInt("SCHEDULING_INTERVAL")
    dataCenter2.setSchedulingInterval(schedulingInterval2)


    val hostList3: List[Host] = PaasSimulation.createHostList()
    val vmsList3: List[Vm] = PaasSimulation.createVmsList()
    val cloudletList3: List[Cloudlet] = PaasSimulation.createCloudlets()
    val dataCenter3 = new DatacenterSimple(simulation, hostList3.asJava, PaasSimulation.getTypeOfAllocation())
    val schedulingInterval3 = config3.getInt("SCHEDULING_INTERVAL")
    dataCenter3.setSchedulingInterval(schedulingInterval3)


    val broker = new DatacenterBrokerSimple(simulation)

    val vmlists = List(vmsList1, vmsList2, vmsList3)
    val cloudletList = List(cloudletList1, cloudletList2, cloudletList3)
    
    broker.submitVmList((vmlists.flatMap(_.zipWithIndex).sortBy(_._2).map(_._1)).asJava)
    broker.submitCloudletList((cloudletList.flatMap(_.zipWithIndex).sortBy(_._2).map(_._1)).asJava)

    val networkTopology = new BriteNetworkTopology()
    simulation.setNetworkTopology(networkTopology)
    networkTopology.addLink(dataCenter1, broker, mainConfig.getDouble("NETWORK_BW"), mainConfig.getDouble("NETWORK_LATENCY"))
    networkTopology.addLink(dataCenter2, broker, mainConfig.getDouble("NETWORK_BW"), mainConfig.getDouble("NETWORK_LATENCY"))
    networkTopology.addLink(dataCenter3, broker, mainConfig.getDouble("NETWORK_BW"), mainConfig.getDouble("NETWORK_LATENCY"))


    simulation.start()

    val finishedCloudlets = broker.getCloudletFinishedList()
    finishedCloudlets.sort(Comparator.comparingLong((cloudlet: Cloudlet) => cloudlet.getVm.getId))
    new CloudletsTableBuilder(finishedCloudlets).build()
    logger.info("**************Exiting CombinedSimulation********************")
  }
}

class CombinedSimulation