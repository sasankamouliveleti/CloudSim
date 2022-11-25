import HelperUtils.{CreateLogger, InfraHelper}
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRoundRobin
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.Cloudlet
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudbus.cloudsim.vms.{Vm, VmCost, VmSimple}
import org.cloudsimplus.autoscaling.{HorizontalVmScaling, HorizontalVmScalingSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util
import java.util.Comparator
import java.util.Comparator.comparingDouble
import java.util.function.Supplier
import scala.collection.immutable.List
import scala.jdk.CollectionConverters.*

object Simulation2 {
  val logger: Logger = CreateLogger(classOf[Simulation2])
  val config: Config = ConfigFactory.load("simulation2.conf").getConfig("simulation2")

  private val HOSTS: Int = config.getInt("HOSTS")
  private val HOST_PES: Int = config.getInt("HOST_PES")

  private val VMS: Int = config.getInt("VMS")
  private val VM_PES: Int = config.getInt("VM_PES")

  private val CLOUDLETS: Int = config.getInt("CLOUDLETS")
  private val CLOUDLET_PES: Int = config.getInt("CLOUDLET_PES")
  private val CLOUDLET_LENGTH: Int = config.getInt("CLOUDLET_LENGTH")

  def main(args: Array[String]): Unit = {
    executeSimulation()
  }

  def executeSimulation(): Unit = {
    val simulation = new CloudSim()


    def createHost(): Host = {
      val peList = new util.ArrayList[Pe](HOST_PES)
      //List of Host's CPUs (Processing Elements, PEs)
      (0 to HOST_PES - 1).map((index) => {
        peList.add(new PeSimple(1000))
      })
      val ram: Long = 2048 //in Megabytes
      val bw: Long = 10000 //in Megabits/s
      val storage: Long = 1000000

      new HostSimple(ram, bw, storage, peList, false)
    }

    def createDatacenter() = {
      val hostList = new util.ArrayList[Host](HOSTS)
      (0 to HOSTS - 1).map((index) => {
        val host = createHost()
        hostList.add(host)
      })
      new DatacenterSimple(simulation, hostList, new VmAllocationPolicyRoundRobin())
    }

    def createListOfScalableVms(vmsNumber: Int): List[Vm] = {
      val newList = {
        (0 to vmsNumber - 1).map((index) => {
          val vm: Vm = createVm(index)
          createHorizontalVmScaling(vm, index)
          vm
        }).toList
      }
      newList
    }

    def createVm(index: Int) = {
      new VmSimple(index, 1000, VM_PES).setRam(512).setBw(1000).setSize(10000).setCloudletScheduler(new CloudletSchedulerTimeShared)
    }

    def isVmOverloaded(vm: Vm) = vm.getCpuPercentUtilization > 0.7

    def createHorizontalVmScaling(vm: Vm, index: Int): Unit = {
      val supplierVM: Supplier[Vm] = new Supplier[Vm] {
        override def get(): Vm = createVm(index)
      }
      val horizontalScaling = new HorizontalVmScalingSimple()
      horizontalScaling.setVmSupplier(supplierVM).setOverloadPredicate(isVmOverloaded)
      vm.setHorizontalScaling(horizontalScaling)
    }

    val vmList = createListOfScalableVms(VMS)
    val cloudletList = InfraHelper.createCloudlets(1000, 1000, CLOUDLET_LENGTH, CLOUDLETS, CLOUDLET_PES)
    val datacenter0 = createDatacenter()
    val broker = new DatacenterBrokerSimple(simulation)

    broker.submitVmList(vmList.asJava)
    broker.submitCloudletList(cloudletList.asJava)
    simulation.start()

    val finishedCloudlets =  broker.getCloudletFinishedList()
    finishedCloudlets.sort(Comparator.comparingLong((cloudlet: Cloudlet) => cloudlet.getVm.getId))
    new CloudletsTableBuilder(finishedCloudlets).build()

    val finshedVms: List[VmSimple] = broker.getVmCreatedList.asScala.toList
    logger.info("The finished vms size is " + finshedVms.length)
    val cost = cloudletList.map((cloudletVal) => {
      if(cloudletVal.isFinished){
        config.getDouble("UsagePerSecond") * cloudletVal.getActualCpuTime() * config.getDouble("BandwidthCost")
      } else {
        0
      }
    }).sum
    logger.info(s"The total cost is ${cost}")

  }
}

class Simulation2