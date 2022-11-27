import HelperUtils.CreateLogger
import ModelSimulations.{CombinedSimulation, IaasSimulation, PaasSimulation, SaasSimulation}
import org.slf4j.Logger

/* To Run all simulations at once mainly used for docker file*/
object RunAllSimulations {
  val logger: Logger = CreateLogger(classOf[RunAllSimulations]) /* Define the logger*/
  def main(args: Array[String]): Unit = {
    logger.info("***************Starting all Simulations****************")
    MainSimulation.executeSimulation()
    SaasSimulation.executeSimulation()
    PaasSimulation.executeSimulation()
    IaasSimulation.executeSimulation()
    CombinedSimulation.executeSimulation()
    logger.info("*****************Exiting all Simulations**************")
  }
}

class RunAllSimulations