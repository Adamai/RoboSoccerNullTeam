package modules;

import java.awt.Rectangle;
import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.Vector2D;

public class CommandPlayer extends Thread {
	private int LOOP_INTERVAL = 100; // 0,1 segundos

	private PlayerCommander commander;
	private PlayerPerception selfPerc;
	private FieldPerception fieldPerc;
	private MatchPerception matchPerc;

	public CommandPlayer(PlayerCommander player) {
		commander = player;
	}

	@Override
	public void run() {
		System.out.println(">> Executando...");
		long nextIteration = System.currentTimeMillis() + LOOP_INTERVAL;
		updatePerceptions();

		switch (selfPerc.getUniformNumber()) {

		case 1:
			acaoGoleiro(nextIteration);
			break;

		case 2:
			acaoArmador(nextIteration, -2); // cima
			break;
		case 3:
			acaoArmador(nextIteration, -1); // cima
			break;
		case 4:
			acaoArmador(nextIteration, 2); // baixo
			break;
		case 5:
			acaoArmador(nextIteration, 1); // baixo
			break;
		case 6:
			acaoAtacante(nextIteration, 1);
			break;
		case 7:
			acaoAtacante(nextIteration, -1);
			break;

		default:
			break;
		}
	}

	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelfBlocking();
		FieldPerception newField = commander.perceiveFieldBlocking();
		MatchPerception newMatch = commander.perceiveMatchBlocking();

		if (newSelf != null)
			this.selfPerc = newSelf;
		if (newField != null)
			this.fieldPerc = newField;
		if (newMatch != null)
			this.matchPerc = newMatch;
	}

	private void turnToPoint(Vector2D point) {
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		commander.doTurnToDirectionBlocking(newDirection);
	}

	private void dash(Vector2D point) {
		if (selfPerc.getPosition().distanceTo(point) <= 1)
			return;

		if (!isAlignToPoint(point, 10))
			turnToPoint(point);
		commander.doDashBlocking(100);
	}

	private void kickToPoint(Vector2D point, double intensity) {
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		double angle = newDirection.angleFrom(selfPerc.getDirection());
		if (angle > 90 || angle < -90) {
			commander.doTurnToDirectionBlocking(newDirection);
			angle = 0;
		}
		commander.doKickBlocking(intensity, angle);
	}

	private boolean isAlignToPoint(Vector2D point, double margin) {
		double angle = point.sub(selfPerc.getPosition()).angleFrom(selfPerc.getDirection());
		return angle < margin && angle > margin * (-1);
	}

	private boolean isPointsAreClose(Vector2D reference, Vector2D point, double margin) {
		return reference.distanceTo(point) <= margin;
	}

	/**
	 * Retorna a percepcao do jogador mais proximo do ponto do primeiro parametro.
	 * 
	 * 
	 */
	private PlayerPerception getClosestPlayerPoint(Vector2D point, EFieldSide side, double margin) {
		ArrayList<PlayerPerception> lp = fieldPerc.getTeamPlayers(side);
		PlayerPerception np = null;
		if (lp != null && !lp.isEmpty()) {
			double dist, temp;
			dist = lp.get(0).getPosition().distanceTo(point);
			np = lp.get(0);
			if (isPointsAreClose(np.getPosition(), point, margin))
				return np;
			for (PlayerPerception p : lp) {
				if (p.getPosition() == null)
					break;
				if (isPointsAreClose(p.getPosition(), point, margin)) {
					return p;
				}
				temp = p.getPosition().distanceTo(point);
				if (temp < dist) {
					dist = temp;
					np = p;
				}
			}

		}
		return np;
	}

	private void acaoGoleiro(long nextIteration) {
		PlayerState goleiroState = PlayerState.STANDBY;
		double xInit = -48, yInit = 0, ballX = 0, ballY = 0;
		EFieldSide side = selfPerc.getSide();
		EFieldSide side2 = EFieldSide.invert(side);
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D ballPos;
		Vector2D closest;
		Vector2D posicaoChute = new Vector2D(36*side.value(), 0);
		double temp = 999;
		Rectangle halfSide = side == EFieldSide.LEFT ? new Rectangle(-52, -34, 26, 68) : new Rectangle(26, -34, 26, 68);
		//USAR HALF SIDE PRA GOLEIRO SEGUIR Y DA BOLA
		Rectangle area = side == EFieldSide.LEFT ? new Rectangle(-52, -20, 16, 40) : new Rectangle(36, -20, 16, 40);
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();

			switch (matchPerc.getState()) {
			case BEFORE_KICK_OFF: // POSICIONA
				commander.doMoveBlocking(xInit, yInit);
				break;
			case KICK_OFF_LEFT:
				break;
			case FREE_KICK_LEFT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case FREE_KICK_RIGHT:
				dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
				break;
			case KICK_IN_RIGHT:

				break;
			case KICK_IN_LEFT:
				break;

			case PLAY_ON:
				ballX = fieldPerc.getBall().getPosition().getX();
				ballY = fieldPerc.getBall().getPosition().getY();
				
				switch (goleiroState) {
					case STANDBY:
						if(halfSide.contains(ballX, ballY)) {		//PREPARAR PARA DEFENDER
							goleiroState = PlayerState.DEFENSIVA;
							break;
						}
						if(isPointsAreClose(selfPerc.getPosition(), initPos, 0.6)) {
							dash(initPos);			//IR PARA POSICAO INICIAL
						} else {
							turnToPoint(ballPos);   // VIRAR PRA BOLA
						}
						
					break;
					case DEFENSIVA:
						if(area.contains(ballX, ballY)) {		//PREPARAR PARA PEGAR A BOLA
							goleiroState = PlayerState.PEGARBOLA;
							break;
						}
						double Ygoal = ballY;
						if(ballPos.getY() > 7) {		//ACOMPANHAR Y DA BOLA EM RELAÇÃO AO GOL
							Ygoal = 6.8;
						} else if(ballPos.getY() < -7) {
							Ygoal = -6.8;
						}
						dash(new Vector2D(selfPerc.getPosition().getX(), Ygoal) );
						if(isPointsAreClose(selfPerc.getPosition(), new Vector2D(selfPerc.getPosition().getX(), Ygoal), 0.1)) {
							turnToPoint(ballPos);   // VIRAR PRA BOLA
						}
					break;
					case PEGARBOLA:
						
					break;
					case PASSAR:
						
					break;
					
				}
				
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					commander.doCatchBlocking(0);
				
					
					//if (!isPointsAreClose(selfPerc.getPosition(), posicaoChute, 0)) {
					//	dash(posicaoChute);
					//	break;
					//}
					
					
					
					// 6 ta marcado? Se não, chuta pra ele
					for (int i = 1; i < 7; i++) {
						if (fieldPerc.getTeamPlayer(side2, i).getPosition()
								.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition()) < temp) {
							closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
							temp = fieldPerc.getTeamPlayer(side2, i).getPosition()
									.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition());
						}
					}
					if (temp > 6) {
						kickToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition(), 70); // CHUTAR
						System.out.println(6);
						temp = 999;
					}

					else {// 7 ta marcado? Se não, chuta pra ele
						temp = 999;
						System.out.println("entrando no else");
						for (int i = 1; i < 7; i++) {
							if (fieldPerc.getTeamPlayer(side2, i).getPosition()
									.distanceTo(fieldPerc.getTeamPlayer(side, 7).getPosition()) < temp) {
								closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
								temp = fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(fieldPerc.getTeamPlayer(side, 7).getPosition());
							}
						}
						if (temp > 6) {
							kickToPoint(fieldPerc.getTeamPlayer(side, 7).getPosition(), 70); // CHUTAR
							System.out.println(7);
							temp = 999;
						} else {
							temp = 999;
							for (int j = 2; j <= 5; j++) {

								for (int i = 1; i < 7; i++) {
									if (fieldPerc.getTeamPlayer(side2, i).getPosition()
											.distanceTo(fieldPerc.getTeamPlayer(side, j).getPosition()) < temp) {
										closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
										temp = fieldPerc.getTeamPlayer(side2, i).getPosition()
												.distanceTo(fieldPerc.getTeamPlayer(side, j).getPosition());
									}
								}

								if (temp > 6) {
									kickToPoint(fieldPerc.getTeamPlayer(side, j).getPosition(), 70); // CHUTAR
									System.out.println(j);
									temp = 999;
								}
							}
						}
					}
				} else if (area.contains(ballX, ballY)) {
					dash(ballPos); // DEFENDER
					
				} else if (!isPointsAreClose(selfPerc.getPosition(), initPos, 5)) {
					dash(initPos); // RECUAR
				}
				else if(halfSide.contains(ballX, ballY)) {
					double Ygoal = ballPos.getY();
					if(ballPos.getY() > 7) {
						Ygoal = 6.7;
					} else if(ballPos.getY() < -7) {
						Ygoal = -6.7;
					}
					dash(new Vector2D(selfPerc.getPosition().getX(), Ygoal) );
				}
				// se a bola esta longe de todos
				else if (!isPointsAreClose(getClosestPlayerPoint(ballPos, side, 3).getPosition(), ballPos, 4)) {
					// mover goleiro para o lado mais proximo da bola
					dash(new Vector2D(xInit * side.value(), (ballPos.getY() / 21) * (3 + yInit) * (side.value())));
					//quando a bola estiver numa certa regiao do campo o goleiro fica no y da bola
					if(isPointsAreClose(selfPerc.getPosition(),ballPos,8)) {
						dash(new Vector2D(selfPerc.getPosition().getX(), ballPos.getY()));
					}
					
				} else {
					turnToPoint(ballPos); // VIRAR PRA BOLA
				}

				break;

			// OUTROS ESTADOS

			default:
				break;
			}
		}
	}

	private void acaoArmador(long nextIteration, int pos) {
		double xInit = -30, yInit = 10 * pos;
		EFieldSide side = selfPerc.getSide();
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D defPos = new Vector2D((-36 * side.value()), 5 * pos);
		Vector2D ballPos, vTemp;
		PlayerPerception pTemp;
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			switch (matchPerc.getState()) {

			case BEFORE_KICK_OFF:
				commander.doMoveBlocking(xInit, yInit);
				break;
			case KICK_OFF_LEFT:
				dash(initPos);
				break;
			case KICK_OFF_RIGHT:
				dash(initPos);
				break;
			case FREE_KICK_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					dash(initPos);
				} else {
					dash(defPos);
				}
				break;
			case FREE_KICK_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					dash(initPos);
				} else {
					dash(defPos);
				}
				break;
			case PLAY_ON:
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					if (selfPerc.getUniformNumber() == 7) {
						// toca para o jogador 7
						vTemp = fieldPerc.getTeamPlayer(side, 3).getPosition();
					} else if (selfPerc.getUniformNumber() == 3) {
						// toca para o jogador 7
						vTemp = fieldPerc.getTeamPlayer(side, 7).getPosition();
					} else if (selfPerc.getUniformNumber() == 4) {
						// toca para o jogador 6
						vTemp = fieldPerc.getTeamPlayer(side, 6).getPosition();
					} else if (selfPerc.getUniformNumber() == 5) {
						// toca para o jogador 6
						vTemp = fieldPerc.getTeamPlayer(side, 6).getPosition();
					} else {
						// nunca entra?
						vTemp = fieldPerc.getTeamPlayer(side, 7).getPosition();
					}

					double intensity;
					// CONCERTAR ISSO AQ ?
					if (isPointsAreClose(selfPerc.getPosition(), vTemp, 30)) {
						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						intensity = (vTempF.magnitude() * 100) / 40;
					} else {
						intensity = 5;
					}
					kickToPoint(vTemp, intensity);
				}
				// SE NENHUM ARMADOR DO TIME ESTA COM A POSSE ENTRA NESSE ELSE
				else {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);

					if (!isPointsAreClose(pTemp.getPosition(), ballPos, 15) && pTemp.getPosition() != defPos) {
						// defender se a bola esta longe de todos os jogadores
						
						dash(defPos);
						
					} else if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						// pega a bola
						dash(ballPos);
					}
					// se a bola está do outro lado do campo e na posse do time
					else if (ballPos.getX() > 0 && (isPointsAreClose(pTemp.getPosition(), ballPos, 5))
							&& selfPerc.getSide() == EFieldSide.LEFT) {
						if (selfPerc.getUniformNumber() == 2) {
							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/3 , yInit * side.value()));
						} else if (selfPerc.getUniformNumber() == 3) {
							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/2 , ((yInit * side.value()) + ballPos.getY())/2));
						} else if (selfPerc.getUniformNumber() == 4) {
							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/2 , yInit * side.value()));
						} else {
							dash(new Vector2D( ((xInit * side.value()) + ballPos.getX())/3 , ((yInit * side.value()) + ballPos.getY())/5));
						}
					} else if (!isPointsAreClose(selfPerc.getPosition(), initPos, 3)) {
						// recua
						dash(initPos);
					} else {
						// olha para a bola
						turnToPoint(ballPos);
					}
				}
				break;

			/* Todos os estados da partida */

			default:
				break;
			}
		}
	}

	private void acaoAtacante(long nextIteration, int pos) {
		double xInit = -15, yInit = 5 * pos;
		EFieldSide side = selfPerc.getSide();
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit);
		Vector2D goalPos = new Vector2D(50 * side.value(), 0);
		Vector2D ballPos;
		Vector2D closest;
		PlayerPerception pTemp;
		double temp = 10;
		double temp2 = 999;
		double tempBlock = 8;
		EFieldSide side2 = EFieldSide.invert(side);

		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();

			switch (matchPerc.getState()) {

			case BEFORE_KICK_OFF:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 6) {
						commander.doMoveBlocking(0, 0.5);
					} else {
						commander.doMoveBlocking(0, -9);
					}
				} else {
					if (selfPerc.getUniformNumber() == 6) {
						commander.doMoveBlocking(xInit, yInit);
					} else {
						commander.doMoveBlocking(xInit, yInit);
					}
				}
				break;
			case KICK_OFF_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 6) {
						dash(new Vector2D(0, -0.5));
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							// turnToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition());
							if (isPointsAreClose(fieldPerc.getTeamPlayer(side, 7).getPosition(), new Vector2D(0, -9),
									0.3)) {
								kickToPoint(fieldPerc.getTeamPlayer(side, 7).getPosition(), 50);
							}
						}
					} else {
						dash(new Vector2D(0, -9));
						if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(0, -9), 0)) {
							turnToPoint(ballPos);
						}
					}
				} else { // não é o time que vai chutar
					dash(initPos);
					if (isPointsAreClose(selfPerc.getPosition(), initPos, 0))
						turnToPoint(ballPos);
				}
				break;
			case KICK_OFF_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {

					if (selfPerc.getUniformNumber() == 6) {
						dash(new Vector2D(0, -0.5));
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							// turnToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition());
							if (isPointsAreClose(fieldPerc.getTeamPlayer(side, 7).getPosition(), new Vector2D(0, -9),
									0.3)) {
								kickToPoint(fieldPerc.getTeamPlayer(side, 7).getPosition(), 45);
							}
						}
					} else {
						dash(new Vector2D(0, -9));
						if (isPointsAreClose(selfPerc.getPosition(), new Vector2D(0, -9), 0)) {
							turnToPoint(ballPos);
						}
					}
				} else {
					dash(initPos);
					if (isPointsAreClose(selfPerc.getPosition(), initPos, 0))
						turnToPoint(ballPos);
				}

				break;
			case FREE_KICK_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							kickToPoint(goalPos, 100);
						}

					} else {
						dash(new Vector2D(ballPos.getX(), 0));
					}
				}
				break;
			case FREE_KICK_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							kickToPoint(goalPos, 100);
						}

					} else {
						dash(new Vector2D(ballPos.getX(), 0));
					}
				}
				break;
			case KICK_IN_RIGHT:
				if (selfPerc.getSide() == EFieldSide.RIGHT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							for (int i = 1; i < 7; i++) {
								if (fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition()) < temp2) {
									closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
									temp2 = fieldPerc.getTeamPlayer(side2, i).getPosition()
											.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition());
								}
							}
							if (temp2 > 6) {
								kickToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition(), 70); // CHUTAR
								System.out.println(6);
								temp2 = 999;
							}

							else {
								temp2 = 999;
								for (int j = 2; j <= 5; j++) {

									for (int i = 1; i < 7; i++) {
										if (fieldPerc.getTeamPlayer(side2, i).getPosition()
												.distanceTo(fieldPerc.getTeamPlayer(side, j).getPosition()) < temp2) {
											closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
											temp2 = fieldPerc.getTeamPlayer(side2, i).getPosition()
													.distanceTo(fieldPerc.getTeamPlayer(side, j).getPosition());
										}
									}

									if (temp2 > 6) {
										kickToPoint(fieldPerc.getTeamPlayer(side, j).getPosition(), 70); // CHUTAR
										System.out.println(j);
										temp2 = 999;
									}
								}
							}

						}
					}
				}
				else {
					dash(initPos);
				}
				
				break;
			case KICK_IN_LEFT:
				if (selfPerc.getSide() == EFieldSide.LEFT) {
					if (selfPerc.getUniformNumber() == 7) {
						dash(ballPos);
						if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
							for (int i = 1; i < 7; i++) {
								if (fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition()) < temp2) {
									closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
									temp2 = fieldPerc.getTeamPlayer(side2, i).getPosition()
											.distanceTo(fieldPerc.getTeamPlayer(side, 6).getPosition());
								}
							}
							if (temp2 > 6) {
								kickToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition(), 70); // CHUTAR
								System.out.println(6);
								temp2 = 999;
							}

							else {
								temp2 = 999;
								for (int j = 2; j <= 5; j++) {

									for (int i = 1; i < 7; i++) {
										if (fieldPerc.getTeamPlayer(side2, i).getPosition()
												.distanceTo(fieldPerc.getTeamPlayer(side, j).getPosition()) < temp2) {
											closest = fieldPerc.getTeamPlayer(side2, i).getPosition();
											temp2 = fieldPerc.getTeamPlayer(side2, i).getPosition()
													.distanceTo(fieldPerc.getTeamPlayer(side, j).getPosition());
										}
									}

									if (temp2 > 6) {
										kickToPoint(fieldPerc.getTeamPlayer(side, j).getPosition(), 70); // CHUTAR
										System.out.println(j);
										temp2 = 999;
									}
								}
							}

						}
					}
				}
				else {
					dash(initPos);
				}
				
				break;
			case PLAY_ON:
				// se o jogador está em posse da bola
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					if (isPointsAreClose(ballPos, goalPos, 30)) {
						// chuta para o gol
						kickToPoint(goalPos, 100);
					} else {
						if (selfPerc.getUniformNumber() == 6) {
							for (int i = 1; i < 7; i++) {
								if (fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(selfPerc.getPosition()) < temp) {
									for (int j = 1; i < 7; i++) {
										if (!(fieldPerc.getTeamPlayer(side2, j).getPosition().distanceTo(
												fieldPerc.getTeamPlayer(side, 7).getPosition()) < tempBlock)) {
											kickToPoint(fieldPerc.getTeamPlayer(side, 7).getPosition(), 60);
											System.out.println("6 passando pro 7");
										}
									}

								}
							}
						} else {
							for (int i = 1; i < 7; i++) {
								if (fieldPerc.getTeamPlayer(side2, i).getPosition()
										.distanceTo(selfPerc.getPosition()) < temp) {
									for (int j = 1; i < 7; i++) {
										if (!(fieldPerc.getTeamPlayer(side2, j).getPosition().distanceTo(
												fieldPerc.getTeamPlayer(side, 6).getPosition()) < tempBlock)) {
											kickToPoint(fieldPerc.getTeamPlayer(side, 6).getPosition(), 60);
											System.out.println("7 passando pro 6");
										}
									}
								}
							}
						}
						// conduz para o gol
						kickToPoint(goalPos, 18);
					}
				} else {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);

					// se o jogador é o mais proximo da bola => vai atrás dela
					if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						// pega a bola
						dash(ballPos);
					}
					// se outro jogador esta com a bola e ela esta do outro lado => avance
					else if (isPointsAreClose(pTemp.getPosition(), ballPos, 6) && ballPos.getX() > 0) {
						if (selfPerc.getUniformNumber() == 6) {
							dash(new Vector2D(pTemp.getPosition().getX(), pTemp.getPosition().getY() + 11));
						} else {
							dash(new Vector2D(pTemp.getPosition().getX(), pTemp.getPosition().getY() - 11));
						}
					}
					// se o jogador saiu muito de posiçao, recua
					else if (!isPointsAreClose(selfPerc.getPosition(), initPos, 3)) {
						// recua
						dash(initPos);
					} else {
						// olha para a bola
						turnToPoint(ballPos);
					}
				}
				break;

			/* Todos os estados da partida */

			default:
				break;
			}
		}
	}
}
