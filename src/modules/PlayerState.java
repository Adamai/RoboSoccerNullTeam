package modules;

public enum PlayerState {
	NULL					(0),
	STANDBY				(1),
	RECEBER				(2),
	PASSAR 				(3),
	CHUTARGOL 			(4),
	DRIBLAR 			(5),
	PEGARBOLA 			(6),
	OFENSIVA 			(7),
	DEFENSIVA 			(8),
	SUPORTE 			(9);
	
	private int value;

	PlayerState(int value){
		this.value = value;
	}
	
	public static PlayerState valueOf(int playerStateValue){
		PlayerState[] values = PlayerState.values();
		for (PlayerState v : values) {
			if(playerStateValue == v.value){
				return v;
			}
		}
		System.err.println("New match state found: "+playerStateValue);
		return NULL;
	}
}
