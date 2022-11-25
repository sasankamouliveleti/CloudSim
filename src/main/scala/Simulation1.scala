import HelperUtils.{CreateLogger, InfraHelper}
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.network.topologies.{BriteNetworkTopology, NetworkTopology}
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.{UtilizationModel, UtilizationModelDynamic, UtilizationModelFull}
import org.cloudbus.cloudsim.vms.{Vm, VmResourceStats, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util
import java.util.Comparator.comparingLong
import java.util.ArrayList
import scala.jdk.CollectionConverters.*

object Simulation1 {

  val logger: Logger = CreateLogger(classOf[Simulation1])

  private val HOSTS = 2
  private val HOST_PES = 8

  private val HOST_START_UP_DELAY = 5

  private val HOST_SHUT_DOWN_DELAY = 3

  private val HOST_START_UP_POWER = 5

  private val HOST_SHUT_DOWN_POWER = 3

  private val VM_PES: Int = 1

  private val NETWORK_BW: Double = 10.0

  private val NETWORK_LATENCY: Double = 10.0

  private val CLOUDLETS: Int = 5
  private val CLOUDLET_PES: Int = 2

  private val STATIC_POWER: Int = 35
  private val MAX_POWER: Int = 50

  private val SCHEDULING_INTERVAL = 10

  private val VMS = 4

  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def executeSimulation(): Unit = {
    logger.info("******************Starting the Simulator 1*********************")
    val simulation = new CloudSim()
    val hostList = new util.ArrayList[Host](HOSTS)

    def createPowerHost(id: Int): Host = {
      val peList = new util.ArrayList[Pe](HOST_PES)
      //List of Host's CPUs (Processing Elements, PEs)
      (0 to HOST_PES - 1).map((index)=>{
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
      (0 to HOSTS - 1).map((index)=>{
        val host = createPowerHost(index)
        hostList.add(host)
      })

      val dc = new DatacenterSimple(simulation, hostList)
      dc.setSchedulingInterval(SCHEDULING_INTERVAL)
      dc
    }

    val vmList: List[Vm] = InfraHelper.createVms(VMS, 1000,VM_PES, 512, 1000, 10000)
    val cloudletList = InfraHelper.createCloudlets(1000, 1000, 100_000,CLOUDLETS,CLOUDLET_PES)
    val datacenter0 = createDatacenter()
    val broker = new DatacenterBrokerSimple(simulation)
    configureNetwork()

    broker.submitVmList(vmList.asJava)
    broker.submitCloudletList(cloudletList.asJava)

    simulation.start()

    new CloudletsTableBuilder(broker.getCloudletCreatedList()).build()


    logger.info("********************Finishing the Simulator 1 *************************")


    def configureNetwork(): Unit = {
      val networkTopology = new BriteNetworkTopology()
      simulation.setNetworkTopology(networkTopology)
      networkTopology.addLink(datacenter0, broker, NETWORK_BW, NETWORK_LATENCY)
    }


    def printVmsCpuUtilizationAndPowerConsumption(): Unit = {
      logger.info(vmList.asJava.size().toString)
      //vmList.asJava.sort(comparingLong((vm: Vm) => vm.getHost.getId))
      logger.info("VM ID | CPU Mean Usage | Power Consumption Mean")
      vmList.asJava.forEach((vm) => {
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
        //System.out.printf("Vm   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W%n", vm.getId, cpuStats.getMean * 100, vmPower)
        logger.info(vm.getId.toString + "|" + (cpuStats.getMean * 100).ceil + "|" + vmPower.ceil)
      })
    }

    printVmsCpuUtilizationAndPowerConsumption()
  }
}

class Simulation1
