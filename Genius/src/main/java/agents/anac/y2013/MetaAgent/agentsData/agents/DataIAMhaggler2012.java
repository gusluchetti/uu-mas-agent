package agents.anac.y2013.MetaAgent.agentsData.agents;

import agents.anac.y2013.MetaAgent.agentsData.AgentData;


public class DataIAMhaggler2012 extends AgentData {
	String data="Node number 1: 10076 observations,    complexity param=0.1895335\n  mean=0.001147019, MSE=0.01921052 \n  left son=2 (3499 obs) right son=3 (6577 obs)\n  Primary splits:\n      DiscountFactor            < 0.875      to the right, improve=0.18953350, (0 missing)\n      UtilityOfFirstOpponentBid < 0.6264208  to the left,  improve=0.04662225, (0 missing)\n      RelevantStdevU            < 0.1292711  to the right, improve=0.04283622, (0 missing)\n      RelevantEU                < 0.7240535  to the left,  improve=0.04105753, (0 missing)\n      WeightStdev               < 0.1379809  to the left,  improve=0.01641318, (0 missing)\n\nNode number 2: 3499 observations,    complexity param=0.02538591\n  mean=-0.08158136, MSE=0.01837965 \n  left son=4 (2939 obs) right son=5 (560 obs)\n  Primary splits:\n      UtilityOfFirstOpponentBid < 0.6264208  to the left,  improve=0.07640801, (0 missing)\n      RelevantStdevU            < 0.09238255 to the right, improve=0.07386368, (0 missing)\n      RelevantEU                < 0.7240535  to the left,  improve=0.06843713, (0 missing)\n      DomainSize                < 37.5       to the right, improve=0.04083313, (0 missing)\n      AvgUtil                   < 0.6707803  to the left,  improve=0.02648469, (0 missing)\n  Surrogate splits:\n      RelevantEU     < 0.7612827  to the left,  agree=0.98, adj=0.875, (0 split)\n      RelevantStdevU < 0.09238255 to the right, agree=0.98, adj=0.875, (0 split)\n      AvgUtil        < 0.7730729  to the left,  agree=0.88, adj=0.250, (0 split)\n      AvgUtilStdev   < 0.1097795  to the right, agree=0.88, adj=0.250, (0 split)\n      WeightStdev    < 0.1379809  to the left,  agree=0.86, adj=0.125, (0 split)\n\nNode number 3: 6577 observations,    complexity param=0.02658984\n  mean=0.04515897, MSE=0.01407446 \n  left son=6 (3358 obs) right son=7 (3219 obs)\n  Primary splits:\n      RelevantStdevU            < 0.1292711  to the right, improve=0.05560111, (0 missing)\n      UtilityOfFirstOpponentBid < 0.518279   to the left,  improve=0.04405038, (0 missing)\n      RelevantEU                < 0.7240535  to the left,  improve=0.03718450, (0 missing)\n      DiscountFactor            < 0.625      to the right, improve=0.03278394, (0 missing)\n      DomainSize                < 104        to the left,  improve=0.03016622, (0 missing)\n  Surrogate splits:\n      UtilityOfFirstOpponentBid < 0.4161004  to the left,  agree=0.915, adj=0.826, (0 split)\n      RelevantEU                < 0.6259309  to the left,  agree=0.766, adj=0.522, (0 split)\n      WeightStdev               < 0.136544   to the left,  agree=0.681, adj=0.348, (0 split)\n      DomainSize                < 282        to the left,  agree=0.681, adj=0.348, (0 split)\n      numOfIssues               < 4.5        to the left,  agree=0.638, adj=0.261, (0 split)\n\nNode number 4: 2939 observations\n  mean=-0.09793944, MSE=0.01873167 \n\nNode number 5: 560 observations\n  mean=0.004269347, MSE=0.007757514 \n\nNode number 6: 3358 observations,    complexity param=0.01225018\n  mean=0.01776987, MSE=0.01606823 \n  left son=12 (1679 obs) right son=13 (1679 obs)\n  Primary splits:\n      DiscountFactor   < 0.625      to the right, improve=0.04394618, (0 missing)\n      numOfIssues      < 3.5        to the left,  improve=0.01767451, (0 missing)\n      ReservationValue < 0.125      to the right, improve=0.01748591, (0 missing)\n      DomainSize       < 104        to the left,  improve=0.01731516, (0 missing)\n      AvgUtilStdev     < 0.3419255  to the right, improve=0.01356561, (0 missing)\n  Surrogate splits:\n      ReservationValue < 0.125      to the right, agree=0.625, adj=0.25, (0 split)\n\nNode number 7: 3219 observations\n  mean=0.07373076, MSE=0.0103957 \n\nNode number 12: 1679 observations\n  mean=-0.008803378, MSE=0.01297732 \n\nNode number 13: 1679 observations\n  mean=0.04434311, MSE=0.01774686 \n\n";
	public String getText() {
		return data;
	}
}
