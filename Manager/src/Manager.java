import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Manager {
	private String[] imageUrls;
	private int numOfWorkers;
	private int numOfMessage;
	private int numOfJobsForWorker;
	private PropertiesCredentials Pc;
	private AWSCredentials Credentials;
	private AmazonS3 S3;
	private String keyBucketName;
	private String BucketName;
	private AmazonSQS AmazonSQS;

	public Manager() {
		try {
			Pc = new PropertiesCredentials(
					Manager.class
							.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Credentials = Pc;
		S3 = new AmazonS3Client(Credentials);
		AmazonSQS = new AmazonSQSClient(Pc);

	}

	public void getOrderFromLocal() {
		System.out.println("Receiving orders from local.\n");

		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				ConstantProvider.LOCAL_TO_MANAGER_QUEUE);
		receiveMessageRequest.setVisibilityTimeout(0);

		String messege = AmazonSQS.receiveMessage(receiveMessageRequest)
				.getMessages().get(0).getBody();
		String[] messegeSplit;
		try {
			numOfJobsForWorker = Integer.parseInt(messege);
			messegeSplit = AmazonSQS.receiveMessage(receiveMessageRequest)
					.getMessages().get(0).getBody().split(" ");

		} catch (Exception e) {
			messegeSplit = messege.split(" ");
			numOfJobsForWorker = Integer.parseInt(AmazonSQS
					.receiveMessage(receiveMessageRequest).getMessages().get(0)
					.getBody());

		}

		// String messegeSplit[] = messages.get(0).getBody().split(" ");
		BucketName = messegeSplit[0];
		keyBucketName = messegeSplit[1];

		System.out.println("Downloading File from server");
		S3Object object = S3.getObject(new GetObjectRequest(BucketName,
				keyBucketName));
		System.out.println("Content-Type: "
				+ object.getObjectMetadata().getContentType());
		try {
			imageUrls = parseInputStream(object.getObjectContent());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		numOfMessage = imageUrls.length - 1;
		// Delete the messege
		// deleteMessegeFromQueue("Deleting order messege",
		// ConstantProvider.LOCAL_TO_MANAGER_QUEUE);
		// S3.deleteObject(BucketName, keyBucketName);

	}

	private void deleteMessegeFromQueue(String messegeToPerform, String Queue) {
		System.out.println(messegeToPerform + ".\n");
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				Queue);
		receiveMessageRequest.setVisibilityTimeout(0);
		String messageRecieptHandle = AmazonSQS
				.receiveMessage(receiveMessageRequest).getMessages().get(0)
				.getReceiptHandle();
		AmazonSQS.deleteMessage(new DeleteMessageRequest(Queue,
				messageRecieptHandle));

	}

	private void setWorkersForJobs() {
		numOfWorkers = numOfMessage / numOfJobsForWorker;
		if (numOfMessage % numOfJobsForWorker != 0)
			numOfWorkers++;

	}

	private void uploadMesseagesToQueue() {
		for (int i = 1; i < imageUrls.length; i++) {
			sendMessege(imageUrls[i], ConstantProvider.MESSEAGES_QUEUE);
		}

	}

	private void sendMessege(String messege, String to) {
		System.out.println("Send messege " + messege + " to " + to);
		AmazonSQS.sendMessage(new SendMessageRequest(to, messege));

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

	public int getNumOfWorkers() {
		return numOfWorkers;
	}

	public void setNumOfWorkers(int numOfWorkers) {
		this.numOfWorkers = numOfWorkers;
	}

	public int getNumOfMessage() {
		return numOfMessage;
	}

	public void setNumOfMessage(int numOfMessage) {
		this.numOfMessage = numOfMessage;
	}

	public int getNumOfJobsForWorker() {
		return numOfJobsForWorker;
	}

	public void setNumOfJobsForWorker(int numOfJobsForWorker) {
		this.numOfJobsForWorker = numOfJobsForWorker;
	}

	public List<Message> receiveMessegeFromQueue(String Queue) {
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				Queue);
		receiveMessageRequest.setVisibilityTimeout(0);
		System.out.println("Messege that recieved is: "
				+ AmazonSQS.receiveMessage(receiveMessageRequest).getMessages()
						.get(0).getBody());
		return AmazonSQS.receiveMessage(receiveMessageRequest).getMessages();

	}

	public void BuildHtmlFile() {
		int numOfMesseage = getNumOfMessage();
		String workersHandle = "";
		while (numOfMesseage > 0) {
			workersHandle = workersHandle
					+ receiveMessegeFromQueue(ConstantProvider.ENCODED_IMAGE);
			deleteMessegeFromQueue("Encoded messeage had been delete",
					ConstantProvider.ENCODED_IMAGE);
			numOfMesseage--;
		}
		String[] workerHandleInArray = workersHandle.split("1qazxsw2@WSXZAQ!");

		try {
			FileWriter fileWriter = new FileWriter("test.html");
			BufferedWriter out = new BufferedWriter(fileWriter);
			out.write("<html>\n<title>OCR</title>\n<body>");
			for (int i = 0; i < workerHandleInArray.length; i = i + 2) {
				out.write("<p>\n");
				out.write("<img src=\"");
				out.write(workerHandleInArray[i]);
				out.write("\"><br/>\n");
				out.write(workerHandleInArray[i + 1] + "\n");
				out.write("<p>\n");

			}

			out.write("</body>\n<html>");
			out.close();

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

	}

	public static void main(String[] args) throws Exception {
		Manager manager = new Manager();
		manager.getOrderFromLocal();
		manager.setWorkersForJobs();
		manager.uploadMesseagesToQueue();
		manager.sendMessege(Integer.toString(manager.getNumOfJobsForWorker()),
				ConstantProvider.MANAGER_TO_WORKER_QUEUE);

		// Busy wait to check if all workers has done there work
		int workersDoneWorking = manager.getNumOfWorkers();
		while (workersDoneWorking > 0) {
			try {
				manager.receiveMessegeFromQueue(ConstantProvider.WORKER_TO_MANAGER_QUEUE);
				System.out.println("Worker " + workersDoneWorking + " Done");
				manager.deleteMessegeFromQueue("Worker done messege delete",
						ConstantProvider.WORKER_TO_MANAGER_QUEUE);
				workersDoneWorking--;

			} catch (Exception e) {
				Thread.sleep(5000);
			}

		}
		System.out.println("Start Building the html file");
		manager.BuildHtmlFile();

		// TODO : check why listing?

	}
}
