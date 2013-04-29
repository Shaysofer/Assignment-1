import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Manager {
	private static String ImageUrl;

	public static void main(String[] args) throws Exception {
		 AmazonEC2 ec2 = ;
		
		int numOfWorkers;
		int numOfJobsForWorker = Integer.parseInt(args[1]);
		int numOfMassage = 0;
		PropertiesCredentials pc = new PropertiesCredentials(
				Manager.class.getResourceAsStream("AwsCredentials.properties"));
		AWSCredentials credentials = pc;

		AmazonS3 s3 = new AmazonS3Client(credentials);

		String directoryName = "Distributed";

		String bucketName = credentials.getAWSAccessKeyId()
				+ "."
				+ directoryName.replace('\\', '_').replace('/', '_')
						.replace(':', '_');
		bucketName = bucketName.toLowerCase();
		String key = "jobs";

		System.out.println("===========================================");
		System.out.println("Getting Started with Amazon S3");
		System.out.println("===========================================\n");

		try {
			System.out.println("Creating bucket " + bucketName + "\n");
			// s3.createBucket(bucketName);

			/*
			 * List the buckets in your account
			 */
			System.out.println("Listing buckets");
			for (Bucket bucket : s3.listBuckets()) {
				System.out.println(" - " + bucket.getName());
			}
			System.out.println();

			// System.out.println("Uploading a new object to S3 from a file\n");
			// File file = new File("TxtImage/imageTxt.txt");
			// s3.putObject(new PutObjectRequest(bucketName, key, file));

			System.out.println("Downloading an object");
			S3Object object = s3
					.getObject(new GetObjectRequest(bucketName, key));
			System.out.println("Content-Type: "
					+ object.getObjectMetadata().getContentType());
			displayTextInputStream(object.getObjectContent());

			System.out.println("Listing objects");
			ObjectListing objectListing = s3
					.listObjects(new ListObjectsRequest().withBucketName(
							bucketName).withPrefix("My"));
			for (S3ObjectSummary objectSummary : objectListing
					.getObjectSummaries()) {
				System.out.println(" - " + objectSummary.getKey() + "  "
						+ "(size = " + objectSummary.getSize() + ")");
			}
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

		// //////SQS//////
		// Listening to imagee url
		AmazonSQS sqs = new AmazonSQSClient(pc);

		System.out.println("===========================================");
		System.out.println("Getting Started with Amazon SQS");
		System.out.println("===========================================\n");

		try {
			// Create a queue
			System.out.println("Creating a new SQS queue called MyQueue.\n");
			CreateQueueRequest createQueueRequest = new CreateQueueRequest(
					"MyQueue" + UUID.randomUUID());
			String myQueueUrl = sqs.createQueue(createQueueRequest)
					.getQueueUrl();

			// List queues
			System.out.println("Listing all queues in your account.\n");
			for (String queueUrl : sqs.listQueues().getQueueUrls()) {
				System.out.println("  QueueUrl: " + queueUrl);
			}
			System.out.println();

			// Send a message
			System.out.println("Sending a message to MyQueue.\n");
			String[] imageTxt = ImageUrl.split("\n");

			// sqs.sendMessage(new SendMessageRequest(myQueueUrl,
			// "This is my message text."));
			numOfMassage = imageTxt.length - 1;
			numOfWorkers = numOfMassage / numOfJobsForWorker;
			if (numOfMassage % numOfJobsForWorker != 0)
				numOfWorkers++;
			for (int i = 1; i < imageTxt.length; i++) {
				sqs.sendMessage(new SendMessageRequest(myQueueUrl, imageTxt[i]));
			}

			// Receive messages
			System.out.println("Receiving messages from MyQueue.\n");
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
					myQueueUrl);
			List<Message> messages = sqs.receiveMessage(receiveMessageRequest)
					.getMessages();

			for (Message message : messages) {
				System.out.println("  Message");
				System.out.println("    MessageId:     "
						+ message.getMessageId());
				System.out.println("    ReceiptHandle: "
						+ message.getReceiptHandle());
				System.out.println("    MD5OfBody:     "
						+ message.getMD5OfBody());
				System.out.println("    Body:          " + message.getBody());
				for (Entry<String, String> entry : message.getAttributes()
						.entrySet()) {
					System.out.println("  Attribute");
					System.out.println("    Name:  " + entry.getKey());
					System.out.println("    Value: " + entry.getValue());
				}
			}
			System.out.println();

			// // Delete a message
			// System.out.println("Deleting a message.\n");
			// String messageRecieptHandle = messages.get(0).getReceiptHandle();
			// sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl,
			// messageRecieptHandle));
			//
			// // Delete a queue
			// System.out.println("Deleting the test queue.\n");
			// sqs.deleteQueue(new DeleteQueueRequest(myQueueUrl));
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

	/**
	 * Displays the contents of the specified input stream as text.
	 * 
	 * @param input
	 *            The input stream to display as text.
	 * 
	 * @throws IOException
	 */
	private static void displayTextInputStream(InputStream input)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;

			System.out.println("    " + line);
			ImageUrl = ImageUrl + "\n" + line;

		}
		System.out.println();
	}
}
