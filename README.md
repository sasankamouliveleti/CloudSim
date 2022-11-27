<h1>Homework 1 - Cloud Organization Simulation</h1>

<h2>Submission by: Sasanka Mouli Subrahmanya Sri Veleti</h2>

<h2>Objective of the Project:</h2>
<p style="font-size: 20px">To Create Cloud Simulators for evaluating executions of applications in cloud datacenters with different characteristics and deployment models.</p>

<h3>Environment and Dependencies used to set up this project</h3>
<ul>
    <li>Operating System: Windows 11 Enterprise, Version - 21H2</li>
    <li>IDE: IntelliJ IDEA Ultimate 2022.2.2</li>
    <li>Java Version - 19.0.1</li>
    <li>Scala Version- 3.1.3, sbt - Version 1.6.2 </li>
    <li>cloudsimplus- 7.3.0</li>
</ul>

Note: This project runs only on top of Java 17+ so make sure you have appropriate JAVA SDK installed.

<h3>Project Structure:</h3>
<p>The project here consists of primarily 5 Simulations namely IaasSimulation, SaasSimulation, PaasSimulation, CombinedSimulation(which uses all Iaas, Saas, Paas datacenters) and another MainSimulation which outputs all the statistics pertaining to a cloud infrastructure.</p>

<h3>Steps to follow to make this project run:</h3>
<ol>
<li>Clone this repository.</li>

```
git clone https://github.com/sasankamouliveleti/Homework3_CloudSim.git
```

<li>Change the required configurations of hosts, cloudlets, and vms in the resources folder for the corresponding simulation</li>
<li>Compile the project using the following command</li>

```
sbt clean compile
```

<li>Run the tests using following command</li>

```
sbt test
```

<li>Run the project using following command</li>

```
sbt run
```

as there are multiple main classes you will be prompted with the class to choose. The description of classes is below
<ol>
<li>Simulation1 - Results out power consumed, cpu utilization metrics and summary of simulation results using default VM Allocation Policy</li>
<li>Simulation2 - Results out the cost of the operation and summary of simulation results using default VM Allocation Policy</li>
<li>Simulation3 - Results out the RAM, CPU usage and different other metrics of VMs</li>
<li>MainSimulation - This is Simulation is culmination of above three simulations</li>
<li>IaasSimulation - This is a simulation of Iaas datacenter, its behavior while allocating resources and handling cloudlets</li>
<li>SaasSimulation - This is a simulation of Saas datacenter, its behavior while allocating resources and handling cloudlets</li>
<li>PaasSimulation - This is a simulation of Paas datacenter, its behavior while allocating resources and handling cloudlets</li>
<li>CombinedSimulation - This is a simulation of ring network topology of Iaas, Saas, Paas datacenters.</li>
</ol>
</ol>

Now, let us explore each Simulation along with its architecture diagram and results. I am leaving out the Simulations 1,2,3 because the MainSimulation is culmination of said 3.



