package Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Local {
	private static PropertiesCredentials Pc;
	private static AWSCredentials Credentials;
	private static AmazonSQS AmazonSqs;
	private static AmazonS3 S3;
	private static String LocalToManagerUrl;
	private static String ManagerToWorkerUrl;
	private static String WorkerToManagerUrl;

	public static void main(String[] args) throws Exception {

		Pc = new PropertiesCredentials(
				Local.class.getResourceAsStream("../AwsCredentials.properties"));
		Credentials = Pc;

		AmazonSqs = new AmazonSQSClient(Pc);

		createQueues();

		int l = 4;

		// ec2 = new AmazonEC2Client(credentials);
		//
		// try {
		// // Basic 32-bit Amazon Linux AMI 1.0 (AMI Id: ami-08728661)
		// RunInstancesRequest request = new RunInstancesRequest(
		// "ami-08728661", 1, 1);
		// request.setInstanceType(InstanceType.T1Micro.toString());
		// List<Instance> instances = ec2.runInstances(request)
		// .getReservation().getInstances();
		// System.out.println("Launch instances: " + instances);
		//
		// } catch (AmazonServiceException ase) {
		// System.out.println("Caught Exception: " + ase.getMessage());
		// System.out.println("Reponse Status Code: " + ase.getStatusCode());
		// System.out.println("Error Code: " + ase.getErrorCode());
		// System.out.println("Request ID: " + ase.getRequestId());
		// }

		S3 = new AmazonS3Client(Credentials);

		String directoryName = "Distributed";

		String bucketName = Credentials.getAWSAccessKeyId()
				+ "."
				+ directoryName.replace('\\', '_').replace('/', '_')
						.replace(':', '_');
		bucketName = bucketName.toLowerCase();
		String key = "tt";

		System.out.println("===========================================");
		System.out.println("Getting Started with Amazon S3");
		System.out.println("===========================================\n");

		try {
			System.out.println("Creating bucket " + bucketName + "\n");
			S3.createBucket(bucketName);

			/*
			 * List the buckets in your account
			 */
			System.out.println("Listing buckets");
			for (Bucket bucket : S3.listBuckets()) {
				System.out.println(" - " + bucket.getName());
			}
			System.out.println();

			System.out.println("Uploading a new object to S3 from a file\n");
			File file = new File("TxtImage/imageTxt.txt");
			S3.putObject(new PutObjectRequest(bucketName, key, file));

			// System.out.println("Downloading an object");
			// S3Object object = s3
			// .getObject(new GetObjectRequest(bucketName, key));
			// System.out.println("Content-Type: "
			// + object.getObjectMetadata().getContentType());
			// displayTextInputStream(object.getObjectContent());
			//
			// System.out.println("Listing objects");
			// ObjectListing objectListing = s3
			// .listObjects(new ListObjectsRequest().withBucketName(
			// bucketName).withPrefix("My"));
			// for (S3ObjectSummary objectSummary : objectListing
			// .getObjectSummaries()) {
			// System.out.println(" - " + objectSummary.getKey() + "  "
			// + "(size = " + objectSummary.getSize() + ")");
			// }
			System.out.println();

			// System.out.println("Deleting an object\n");
			// s3.deleteObject(bucketName, key);
			//
			// System.out.println("Deleting bucket " + bucketName + "\n");
			// s3.deleteBucket(bucketName);
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
		// ////////SQS//////
		// AmazonSQS sqs = new AmazonSQSClient(Pc);
		//
		// System.out.println("===========================================");
		// System.out.println("Getting Started with Amazon SQS");
		// System.out.println("===========================================\n");
		//
		// try {
		//
		// // List queues
		// System.out.println("Listing all queues in your account.\n");
		// for (String queueUrl : sqs.listQueues().getQueueUrls()) {
		// System.out.println("  QueueUrl: " + queueUrl);
		// }
		// System.out.println();
		//
		//
		// AmazonEC2 ec2;
		//
		// ec2 = new AmazonEC2Client(Credentials);
		//
		// try {
		// // Basic 32-bit Amazon Linux AMI 1.0 (AMI Id: ami-08728661)
		// RunInstancesRequest request = new RunInstancesRequest(
		// "ami-08728661", 1, 1);
		// request.setInstanceType(InstanceType.T1Micro.toString());
		// request.setUserData(userData());
		// List<Instance> instances = ec2.runInstances(request)
		// .getReservation().getInstances();
		// System.out.println("Launch instances: " + instances);
		//
		// } catch (AmazonServiceException ase) {
		// System.out.println("Caught Exception: " + ase.getMessage());
		// System.out.println("Reponse Status Code: "
		// + ase.getStatusCode());
		// System.out.println("Error Code: " + ase.getErrorCode());
		// System.out.println("Request ID: " + ase.getRequestId());
		// }

		// Receive messages
		// System.out.println("Receiving messages from MyQueue.\n");
		// ReceiveMessageRequest receiveMessageRequest = new
		// ReceiveMessageRequest(
		// myQueueUrl);
		// List<Message> messages = sqs.receiveMessage(receiveMessageRequest)
		// .getMessages();
		// for (Message message : messages) {
		// System.out.println("  Message");
		// System.out.println("    MessageId:     "
		// + message.getMessageId());
		// System.out.println("    ReceiptHandle: "
		// + message.getReceiptHandle());
		// System.out.println("    MD5OfBody:     "
		// + message.getMD5OfBody());
		// System.out.println("    Body:          " + message.getBody());
		// for (Entry<String, String> entry : message.getAttributes()
		// .entrySet()) {
		// System.out.println("  Attribute");
		// System.out.println("    Name:  " + entry.getKey());
		// System.out.println("    Value: " + entry.getValue());
		// }
		// }
		// System.out.println();

		// Delete a message
		// System.out.println("Deleting a message.\n");
		// String messageRecieptHandle = messages.get(0).getReceiptHandle();
		// sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl,
		// messageRecieptHandle));

		// Delete a queue
		// System.out.println("Deleting the test queue.\n");
		// sqs.deleteQueue(new DeleteQueueRequest(myQueueUrl));
		// } catch (AmazonServiceException ase) {
		// System.out
		// .println("Caught an AmazonServiceException, which means your request made it "
		// +
		// "to Amazon SQS, but was rejected with an error response for some reason.");
		// System.out.println("Error Message:    " + ase.getMessage());
		// System.out.println("HTTP Status Code: " + ase.getStatusCode());
		// System.out.println("AWS Error Code:   " + ase.getErrorCode());
		// System.out.println("Error Type:       " + ase.getErrorType());
		// System.out.println("Request ID:       " + ase.getRequestId());
		// } catch (AmazonClientException ace) {
		// System.out
		// .println("Caught an AmazonClientException, which means the client encountered "
		// +
		// "a serious internal problem while trying to communicate with SQS, such as not "
		// + "being able to access the network.");
		// System.out.println("Error Message: " + ace.getMessage());
		// }

	}

	public static String userData() {
		String s;
		s = "#!/bin/bash\n+";

		// set -e -x
		// export DEBIAN_FRONTEND=noninteractive
		// apt-get update && apt-get upgrade -y
		// tasksel install lamp-server
		// echo "Please remember to set the MySQL root password!"";
		return s;
	}

	/**
	 * Displays the contents of the specified input stream as text.
	 * 
	 * @param input
	 *            The input stream to display as text.
	 * 
	 * @throws IOException
	 */
	static void createQueues() {
		System.out.println("Create Queues");
		CreateQueueRequest LocalToManagerQueueRequest = new CreateQueueRequest()
				.withQueueName("LocalToManagerQueue");
		LocalToManagerUrl = AmazonSqs.createQueue(LocalToManagerQueueRequest)
				.getQueueUrl();

		CreateQueueRequest ManagerToWorkerQueueRequest = new CreateQueueRequest()
				.withQueueName("ManagerToWorkerQueue");
		ManagerToWorkerUrl = AmazonSqs.createQueue(ManagerToWorkerQueueRequest)
				.getQueueUrl();

		CreateQueueRequest WorkerToManagerQueueRequest = new CreateQueueRequest()
				.withQueueName("WorkerToManager");
		WorkerToManagerUrl = AmazonSqs.createQueue(WorkerToManagerQueueRequest)
				.getQueueUrl();

	}

	private static void displayTextInputStream(InputStream input)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;

			System.out.println("    " + line);
		}
		System.out.println();
	}
}
