package Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Local {
	private PropertiesCredentials Pc;
	private AWSCredentials Credentials;
	private AmazonSQS AmazonSqs;
	private AmazonS3 S3;
	private AmazonEC2Client ec2;
	private String LocalToManagerUrl;
	private String ManagerToWorkerUrl;
	private String WorkerToManagerUrl;
	private String MesseagesQueueUrl;
	private String WorkerToManagerFinish;
	private String ManagerDone;
	private String BucketName;
	private String KeyBucketName;

	public Local() {
		try {
			Pc = new PropertiesCredentials(
					Local.class
							.getResourceAsStream("../AwsCredentials.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Credentials = Pc;
		AmazonSqs = new AmazonSQSClient(Pc);
		S3 = new AmazonS3Client(Credentials);

	}

	public String userData() {
		String s = "#! /bin/bash\n"
				+ "cd /home/ec2-user/\n"
				+ "wget https://s3.amazonaws.com/akiajeww7utg6gq2srmq.distributed/manager\n"
				+ "java -jar manager >log.txt\n";

		return new String(Base64.encodeBase64(s.getBytes()));
	}
	public void createBucketAndUploadFileJar(File file, String key) {

		BucketName = Credentials.getAWSAccessKeyId()
				+ "."
				+ ConstantProvider.DIRECTORY_NAME.replace('\\', '_')
						.replace('/', '_').replace(':', '_');
		BucketName = BucketName.toLowerCase();

		try {
			System.out.println("Creating bucket " + BucketName + "\n");
			S3.createBucket(BucketName);

			/*
			 * List the buckets in your account
			 */
			System.out.println("Listing buckets");
			System.out.println("Uploading " + key + " to S3 from a file\n");
			PutObjectRequest putRequest = new PutObjectRequest(BucketName, key,
					file);
			putRequest.setCannedAcl(CannedAccessControlList.PublicReadWrite);
			S3.putObject(putRequest);
		} catch (AmazonServiceException ase) {
			System.out
					.println("Caught an AmazonServiceException, which means your request made it "
							+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out
					.println("Caught an AmazonClientException, which means the client encountered "
							+ "a serious internal problem while trying to communicate with S3, "
							+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}

	}

	public void createBucketAndUploadFile(File file) {

		BucketName = Credentials.getAWSAccessKeyId()
				+ "."
				+ ConstantProvider.DIRECTORY_NAME.replace('\\', '_')
						.replace('/', '_').replace(':', '_');
		BucketName = BucketName.toLowerCase();
		KeyBucketName = "distributed";

		System.out.println("===========================================");
		System.out.println("Getting Started with Amazon S3");
		System.out.println("===========================================\n");

		try {
			System.out.println("Creating bucket " + BucketName + "\n");
			S3.createBucket(BucketName);

			/*
			 * List the buckets in your account
			 */
			System.out.println("Listing buckets");
			for (Bucket bucket : S3.listBuckets()) {
				System.out.println(" - " + bucket.getName());
			}
			System.out.println();

			System.out.println("Uploading a new object to S3 from a file\n");
			S3.putObject(new PutObjectRequest(BucketName, KeyBucketName, file));

			System.out.println();

		} catch (AmazonServiceException ase) {
			System.out
					.println("Caught an AmazonServiceException, which means your request made it "
							+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out
					.println("Caught an AmazonClientException, which means the client encountered "
							+ "a serious internal problem while trying to communicate with S3, "
							+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}

	}

	public void createQueues() {
		System.out.println("Create Queues");
		try {
			CreateQueueRequest LocalToManagerQueueRequest = new CreateQueueRequest()
					.withQueueName("LocalToManagerQueue");
			LocalToManagerUrl = AmazonSqs.createQueue(
					LocalToManagerQueueRequest).getQueueUrl();

			CreateQueueRequest ManagerToWorkerQueueRequest = new CreateQueueRequest()
					.withQueueName("ManagerToWorkerQueue");
			ManagerToWorkerUrl = AmazonSqs.createQueue(
					ManagerToWorkerQueueRequest).getQueueUrl();

			CreateQueueRequest WorkerToManagerQueueRequest = new CreateQueueRequest()
					.withQueueName("WorkerToManager");
			WorkerToManagerUrl = AmazonSqs.createQueue(
					WorkerToManagerQueueRequest).getQueueUrl();

			CreateQueueRequest MesseagesQueueUrlRequest = new CreateQueueRequest()
					.withQueueName("MesseagesQueue");
			MesseagesQueueUrl = AmazonSqs.createQueue(MesseagesQueueUrlRequest)
					.getQueueUrl();

			CreateQueueRequest WorkerToManagerFinishQueueUrlRequest = new CreateQueueRequest()
					.withQueueName("WorkerToManagerFinish");
			WorkerToManagerFinish = AmazonSqs.createQueue(
					WorkerToManagerFinishQueueUrlRequest).getQueueUrl();
			CreateQueueRequest ManagerDoneQueueRequest = new CreateQueueRequest()
					.withQueueName("ManagerDone");
			ManagerDone = AmazonSqs.createQueue(ManagerDoneQueueRequest)
					.getQueueUrl();

			System.out.println("Queues Created");

		} catch (AmazonServiceException ase) {
			System.out
					.println("Caught an AmazonServiceException, which means your request made it "
							+ "to Amazon SQS, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out
					.println("Caught an AmazonClientException, which means the client encountered "
							+ "a serious internal problem while trying to communicate with SQS, such as not "
							+ "being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}

	}

	public void deleteQueues() throws InterruptedException {

		System.out.println("Deleting the queues.\n");
		AmazonSqs.deleteQueue(new DeleteQueueRequest(
				ConstantProvider.WORKER_TO_MANAGER_FINISH));
		AmazonSqs.deleteQueue(new DeleteQueueRequest(
				ConstantProvider.MANAGER_DONE));
		Thread.sleep(60000);

	}

	public void sendMessege(String messege, String to) {
		AmazonSqs.sendMessage(new SendMessageRequest(to, messege));

	}

	public PropertiesCredentials getPc() {
		return Pc;
	}

	public void setPc(PropertiesCredentials pc) {
		Pc = pc;
	}

	public AWSCredentials getCredentials() {
		return Credentials;
	}

	public void setCredentials(AWSCredentials credentials) {
		Credentials = credentials;
	}

	public AmazonSQS getAmazonSqs() {
		return AmazonSqs;
	}

	public void setAmazonSqs(AmazonSQS amazonSqs) {
		AmazonSqs = amazonSqs;
	}

	public AmazonS3 getS3() {
		return S3;
	}

	public void setS3(AmazonS3 s3) {
		S3 = s3;
	}

	public String getLocalToManagerUrl() {
		return LocalToManagerUrl;
	}

	public void setLocalToManagerUrl(String localToManagerUrl) {
		LocalToManagerUrl = localToManagerUrl;
	}

	public String getManagerToWorkerUrl() {
		return ManagerToWorkerUrl;
	}

	public void setManagerToWorkerUrl(String managerToWorkerUrl) {
		ManagerToWorkerUrl = managerToWorkerUrl;
	}

	public String getWorkerToManagerUrl() {
		return WorkerToManagerUrl;
	}

	public void setWorkerToManagerUrl(String workerToManagerUrl) {
		WorkerToManagerUrl = workerToManagerUrl;
	}

	public String getBucketName() {
		return BucketName;
	}

	public void setBucketName(String bucketName) {
		BucketName = bucketName;
	}

	public String getKeyBucketName() {
		return KeyBucketName;
	}

	public void setKeyBucketName(String keyBucketName) {
		KeyBucketName = keyBucketName;
	}

	public String[] downloadFileFromServer() {
		String[] AnswearMessege = null;
		System.out.println("Downloading File from server");
		S3Object object = S3.getObject(new GetObjectRequest(BucketName,
				"html.txt"));
		System.out.println("Content-Type: "
				+ object.getObjectMetadata().getContentType());
		try {
			AnswearMessege = parseInputStream(object.getObjectContent());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return AnswearMessege;

	}

	private String[] parseInputStream(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String ImageUrls = "";
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			ImageUrls = ImageUrls + "\n" + line;

		}
		String[] ans = ImageUrls.split("\n");

		return ans;
	}

	@SuppressWarnings("unused")
	private void deleteMessegeFromQueue(String messegeToPerform, String Queue) {
		System.out.println(messegeToPerform + ".\n");
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				Queue);
		String messageRecieptHandle = AmazonSqs
				.receiveMessage(receiveMessageRequest).getMessages().get(0)
				.getReceiptHandle();
		AmazonSqs.deleteMessage(new DeleteMessageRequest(Queue,
				messageRecieptHandle));

	}
	
	private List<Instance> startManager(){
		
			ec2 = new AmazonEC2Client(Pc);
			RunInstancesRequest request = new RunInstancesRequest(
					"ami-3275ee5b", 1, 1);
			request.setKeyName("oren1");
			request.setInstanceType(InstanceType.T1Micro.toString());
			request.setUserData(userData());
			List<Instance> manager = ec2.runInstances(request)
					.getReservation().getInstances();
			return manager;
			
			// System.out.println("Launch instances: " + instances);
		}
	

	

	public static void main(String[] args) throws Exception {
		ArrayList<String> man = new ArrayList<String>();
		Local local = new Local();
		File file = new File("TxtImage/imageTxt.txt");
		local.createBucketAndUploadFile(file);
		File file1 = new File("check2.jar");
		// local.createBucketAndUploadFileJar(file1,"check2");
		file1 = new File("libAspriseOCR.so");
		// local.createBucketAndUploadFileJar(file1,"libAspriseOCR.so");
		File file2 = new File("manager.jar");
		local.createBucketAndUploadFileJar(file2,"manager");
		List<Instance> manager = local.startManager();

		// local.deleteQueues();
		local.createQueues();

		String inputFileName = "";
		String outputFileName = "";
		String numOfWorkers = "";

		// if (args.length > 0) {
		// inputFileName = args[1];
		// outputFileName = args[2];
		// numOfWorkers = argshome/ec2-user/[3];
		//
		// }
		local.sendMessege(
				local.getBucketName() + " " + local.getKeyBucketName() + " 3",
				ConstantProvider.LOCAL_TO_MANAGER_QUEUE);
		// Wait for finish answer
		while (true) {
			try {
				ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
						ConstantProvider.MANAGER_DONE);
			List<Message> messages = local.getAmazonSqs()
						.receiveMessage(receiveMessageRequest).getMessages();
				if (messages.get(0).getBody().equals("Done")){
					man.add(manager.get(0).getInstanceId());
					local.ec2.terminateInstances(new TerminateInstancesRequest(man));
					break;
				}
					
					else
					Thread.sleep(1000);

			} catch (Exception e) {
				Thread.sleep(1000);
			}

		}
		String MissionComplete[] = local.downloadFileFromServer();

		try {
			FileWriter fileWriter = new FileWriter("html.txt");
			BufferedWriter out = new BufferedWriter(fileWriter);
			for (String s : MissionComplete) {
				out.write(s);
			}
			out.close();

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	//local.deleteQueues();

	}

}
