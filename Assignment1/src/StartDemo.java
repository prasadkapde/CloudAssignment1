import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.model.Instance;

public class StartDemo {
	public static void main(String[] args) throws IOException,
			InterruptedException {
		VirtualMachine vm = new VirtualMachine();
		vm.createVirtualMachine(2);
		List<Instance> instanceList = vm.getInstanceList();
		Map<String, String> volumeIdMap = new HashMap<String, String>();

		for (Instance ins : instanceList) {
			System.out.println(ins.getInstanceId());
			vm.isRunning(ins.getInstanceId());
			String volumeId = vm.attachPersistentDataVolume(
					ins.getInstanceId(), ins.getPlacement()
							.getAvailabilityZone());

			volumeIdMap.put(ins.getInstanceId(), volumeId);
		}
		Thread.sleep(5000);

		for (Instance ins : instanceList) {
			String volumeId = volumeIdMap.get(ins.getInstanceId());
			vm.detachDataVolume(ins.getInstanceId(), volumeId);
		}
	}
}
