import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.model.Instance;

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
		Map<String, String> AMI_IdMap = new HashMap<String, String>();
		Map<String, String> amiVolumeMap = new HashMap<String, String>();

		// ///////////////////////////////////////////////////////////////////
		// Create and attach volumes on these instances //
		// ///////////////////////////////////////////////////////////////////

		for (Instance ins : instanceList) {
			System.out.println(ins.getInstanceId());
			vm.isRunning(ins.getInstanceId());
			String volumeId = vm.createDataVolume(
					ins.getInstanceId(), ins.getPlacement()
							.getAvailabilityZone());

			volumeIdMap.put(ins.getInstanceId(), volumeId);
		}

		Thread.sleep(5000);
		// ///////////////////////////////////////////////////////////////////
		// Check CPU utilization and create AMI for idle VMs //
		// ///////////////////////////////////////////////////////////////////

		boolean flag = true;
		String instanceId = null;
		while (flag) {
			for (Instance ins : instanceList) {
				instanceId = ins.getInstanceId();
				double cpuUtilization = vm.cloudWatch(instanceId);
				if (cpuUtilization < 5.0) {
					String volumeId = volumeIdMap.get(instanceId);
					vm.detachDataVolume(instanceId, volumeId);
					String imageId = vm.createAMI(instanceId);
					AMI_IdMap.put(instanceId, imageId);
					amiVolumeMap.put(imageId, volumeId);
					vm.terminateInstance(instanceId);
					flag = false;
				}
			}
		}
		instanceList.remove(instanceId);

		// ///////////////////////////////////////////////////////////////////
		// After 5 pm //
		// ///////////////////////////////////////////////////////////////////

		// ///////////////////////////////////////////////////////////////////
		// Detach Volumes and Create Snapshot//
		// ///////////////////////////////////////////////////////////////////

		for (Instance ins : instanceList) {
			String volumeId = volumeIdMap.get(ins.getInstanceId());
			vm.detachDataVolume(ins.getInstanceId(), volumeId);
			String imageId = vm.createAMI(ins.getInstanceId());
			AMI_IdMap.put(ins.getInstanceId(), imageId);
			amiVolumeMap.put(imageId, volumeId);
			vm.terminateInstance(ins.getInstanceId());
		}

		// ///////////////////////////////////////////////////////////////////
		// Next Day //
		// ///////////////////////////////////////////////////////////////////

		for (Map.Entry<String, String> entry : AMI_IdMap.entrySet()) {
			vm.createVirtualMachine(entry.getValue(), 1);
		}
		List<Instance> instanceList1 = vm.getInstanceList();
		for(Instance instance : instanceList1){
			String volumeId = amiVolumeMap.get(instance.getImageId());
			vm.attachDataVolume(volumeId, instance.getInstanceId());

		}
	}
}
