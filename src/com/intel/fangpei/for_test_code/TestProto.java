package com.intel.fangpei.for_test_code;
import com.clusterwork.protocol.MasterNodeProtocolProtos.Issued;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
public class TestProto {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Issued.Builder builder = Issued.newBuilder();
		builder.setVersion(0);
		builder.setIdentity(12345);
		builder.setTerminal(builder.getTerminal().ADMIN);
		builder.setCommand(2);
		builder.setArgs(ByteString.copyFromUtf8("abc"));
		Issued issued = builder.build();
		
		byte[] result = issued.toByteArray();
		try {
			System.out.println(Issued.parseFrom(result));
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
