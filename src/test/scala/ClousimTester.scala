package scala

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import HelperUtils.{Constants, InfraHelper}
import com.typesafe.config.ConfigFactory

class CloudsimTester extends AnyFlatSpec with Matchers {
  behavior of "Various helper methods and config parameters"

  it should "the config parameter network bandwidth should correctly assigned" in {
    val config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")
    config.getDouble("NETWORK_BW") shouldBe 10.0
  }

  it should "the config parameter network latency should correctly assigned" in {
    val config = ConfigFactory.load("application.conf").getConfig("applicationconfigparams")
    config.getDouble("NETWORK_LATENCY") shouldBe 10.0
  }

  it should "create correct number of cloudlets" in {
    val config = ConfigFactory.load("simulation1.conf").getConfig("simulation1")
    val clouletsVal = InfraHelper.createCloudlets(config)
    clouletsVal.size shouldBe config.getInt("CLOUDLETS_COUNT")
  }

  it should "create correct number of hosts" in {
    val config = ConfigFactory.load("simulation2.conf").getConfig("simulation2")
    val hostsVal = InfraHelper.createHostList(config)
    hostsVal.size shouldBe config.getInt("HOSTS_COUNT")
  }

  it should "create correct number of vms" in {
    val config = ConfigFactory.load("simulation3.conf").getConfig("simulation3")
    val vmsVal = InfraHelper.createVmsList(config)
    vmsVal.size shouldBe config.getInt("VMS_COUNT")
  }
}