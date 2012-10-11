import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;

public class StartDemo {
	public static void main(String[] args) throws IOException,
			InterruptedException {
		VirtualMachine vm = new VirtualMachine();
		// ///////////////////////////////////////////////////////////////////
		// Create New VMs //
		// ///////////////////////////////////////////////////////////////////
		vm.createVirtualMachine("ami-ca32efa3", 2);

		List<Instance> instanceList = vm.getInstanceList();
		Map<String, String> volumeIdMap = new HashMap<String, String>();

		// ///////////////////////////////////////////////////////////////////
		// Create and attach volumes on these instances //
		// ///////////////////////////////////////////////////////////////////

		for (Instance ins : instanceList) {
			System.out.println(ins.getInstanceId());
			vm.isRunning(ins.getInstanceId());
			String volumeId = vm.attachPersistentDataVolume(
					ins.getInstanceId(), ins.getPlacement()
							.getAvailabilityZone());

			volumeIdMap.put(ins.getInstanceId(), volumeId);
		}

		Thread.sleep(5000);
		// ///////////////////////////////////////////////////////////////////
		// Detach Volumes //
		// ///////////////////////////////////////////////////////////////////

		for (Instance ins : instanceList) {
			String volumeId = volumeIdMap.get(ins.getInstanceId());
			vm.detachDataVolume(ins.getInstanceId(), volumeId);
		}
		// ///////////////////////////////////////////////////////////////////

	}
}
