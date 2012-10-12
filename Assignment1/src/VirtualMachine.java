import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3Client;

public class VirtualMachine {
	static AmazonEC2 ec2;
	static AmazonS3Client s3;

	public void createVirtualMachine(String imageId, int numberOfVirualMachines)
			throws IOException {
		AWSCredentials credentials = new PropertiesCredentials(
				VirtualMachine.class
						.getResourceAsStream("AwsCredentials.properties"));
		ec2 = new AmazonEC2Client(credentials);
		// String imageId = "ami-ca32efa3";
		int minInstanceCount = numberOfVirualMachines; // create 1 instance
		int maxInstanceCount = numberOfVirualMachines;
		RunInstancesRequest rir = new RunInstancesRequest(imageId,
				minInstanceCount, maxInstanceCount);
		RunInstancesResult result = ec2.runInstances(rir);

		// get instanceId from the result
		List<Instance> resultInstance = result.getReservation().getInstances();

		for (Instance ins : resultInstance) {

			System.out.println("New instance has been created: "
					+ ins.getInstanceId());

		}

		/*********************************************
		 * 
		 * #7 Create a 'tag' for the new instance.
		 * 
		 *********************************************/
		System.out.println("#6 Create a 'tag' for the new instance.");
		List<String> resources = new LinkedList<String>();
		List<Tag> tags = new LinkedList<Tag>();

		Tag nameTag = new Tag("Name", "PrasadKapde");
		for (Instance ins : resultInstance) {
			resources.add(ins.getInstanceId());
			tags.add(nameTag);
		}
		CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
		ec2.createTags(ctr);

	}

	public String attachPersistentDataVolume(String instanceId,
			String availibilityZone) throws IOException {
		CreateVolumeRequest cvr = new CreateVolumeRequest();

		cvr.setAvailabilityZone(availibilityZone);
		cvr.setSize(1); // size = 1 gigabytes
		CreateVolumeResult volumeResult = ec2.createVolume(cvr);
		String createdVolumeId = volumeResult.getVolume().getVolumeId();
		AttachVolumeRequest avr = new AttachVolumeRequest();
		avr.setVolumeId(createdVolumeId);
		avr.setInstanceId(instanceId);
		avr.setDevice("/dev/sdf");
		ec2.attachVolume(avr);
		System.out.println("Attached volume id: " + createdVolumeId);
		return createdVolumeId;

	}

	public void detachDataVolume(String instanceId, String createdVolumeId) {
		DetachVolumeRequest dvr = new DetachVolumeRequest();
		dvr.setVolumeId(createdVolumeId);
		dvr.setInstanceId(instanceId);
		ec2.detachVolume(dvr);
	}

	public String createAMI(String instanceId) throws IOException {
		AWSCredentials credentials = new PropertiesCredentials(
				VirtualMachine.class
						.getResourceAsStream("AwsCredentials.properties"));
		ec2 = new AmazonEC2Client(credentials);

		CreateImageRequest cir = new CreateImageRequest();
		cir.setInstanceId(instanceId);
		cir.setName("hw_test_ami");
		CreateImageResult createImageResult = ec2.createImage(cir);
		String createdImageId = createImageResult.getImageId();

		System.out.println("Sent creating AMI request. AMI id="
				+ createdImageId);
		return createdImageId;
	}

	public void loadAMI(String imageId) {
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest(
				imageId, 1, 1);
		ec2.runInstances(runInstancesRequest);

	}

	public List<Instance> getInstanceList() throws IOException {
		List<Instance> instanceList = new ArrayList<Instance>();
		AWSCredentials credentials = new PropertiesCredentials(
				VirtualMachine.class
						.getResourceAsStream("AwsCredentials.properties"));
		ec2 = new AmazonEC2Client(credentials);

		DescribeInstancesResult describeInstancesResult = ec2
				.describeInstances();
		List<Reservation> reservations = describeInstancesResult
				.getReservations();
		Set<Instance> instances = new HashSet<Instance>();
		for (Reservation reservation : reservations) {

			instances.addAll(reservation.getInstances());

		}
		for (Instance ins : instances) {
			if (ins.getState().getName().equals("running")
					|| ins.getState().getName().equals("pending")) {
				instanceList.add(ins);
			}
		}

		return instanceList;
	}

	public void isRunning(String instanceId) {
		DescribeInstancesResult describeInstancesResult1;

		do {
			DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
			describeInstancesRequest.withInstanceIds(instanceId);
			describeInstancesResult1 = ec2
					.describeInstances(describeInstancesRequest);

		} while (!describeInstancesResult1.getReservations().get(0)
				.getInstances().get(0).getState().getName().equals("running"));

	}

	public double cloudWatch(String instanceId) throws IOException {
		double average = 0;
		AWSCredentials credentials = new PropertiesCredentials(
				VirtualMachine.class
						.getResourceAsStream("AwsCredentials.properties"));

		AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(
				credentials);

		GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
		statRequest.setNamespace("AWS/EC2"); // namespace
		statRequest.setPeriod(60); // period of data
		ArrayList<String> stats = new ArrayList<String>();
		stats.add("Average");
		statRequest.setStatistics(stats);
		statRequest.setMetricName("CPUUtilization");
		GregorianCalendar calendar = new GregorianCalendar(
				TimeZone.getTimeZone("UTC"));
		calendar.add(GregorianCalendar.SECOND,
				-1 * calendar.get(GregorianCalendar.SECOND)); // 1 second
																// ago
		Date endTime = calendar.getTime();
		calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
		Date startTime = calendar.getTime();
		statRequest.setStartTime(startTime);
		statRequest.setEndTime(endTime);
		ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
		dimensions.add(new Dimension().withName("InstanceId").withValue(
				instanceId));
		statRequest.setDimensions(dimensions);

		// get statistics
		GetMetricStatisticsResult statResult = cloudWatch
				.getMetricStatistics(statRequest);

		// display
		System.out.println(statResult.toString());
		List<Datapoint> dataList = statResult.getDatapoints();
		Double averageCPU = null;
		Date timeStamp = null;
		for (Datapoint data : dataList) {
			averageCPU = data.getAverage();
			timeStamp = data.getTimestamp();
			average = averageCPU;
		}

		return average;
	}

	public void terminateInstance(String instanceId) {
		TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
		terminateInstancesRequest.withInstanceIds(instanceId);
		ec2.terminateInstances(terminateInstancesRequest);
	}
}
