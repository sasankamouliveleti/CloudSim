package HelperUtils

import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.utilizationmodels.{UtilizationModel, UtilizationModelDynamic, UtilizationModelFull}
import org.cloudbus.cloudsim.vms.VmSimple

object InfraHelper {

  def createVms(vmCount: Int, vmMips: Long, vmPes: Int, vmRam: Long, vmBD: Long, vmSize: Long): List[VmSimple] = {
    val vmsList = (1 to vmCount).map(index => {
      val vm = new VmSimple(vmMips, vmPes).setRam(vmRam).setBw(vmBD).setSize(vmSize).asInstanceOf[VmSimple]
      vm.enableUtilizationStats()
      vm
    }).toList
    vmsList
  }

  def createCloudlets(fileSize: Long, outpuSize: Long, length: Long, cloudletCount: Int, cloudletPes: Int)= {
    val utilization: UtilizationModelDynamic = new UtilizationModelDynamic(0.2)
    val cloudletList = (1 to cloudletCount).map(index => {
      val cloudlet: Cloudlet = new CloudletSimple(index, length, cloudletPes).setFileSize(fileSize).setOutputSize(outpuSize).setUtilizationModelCpu(new UtilizationModelFull).setUtilizationModelRam(utilization).setUtilizationModelBw(utilization)
      cloudlet
    }).toList
    cloudletList
  }
}

