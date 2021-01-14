import java.time.OffsetDateTime

import com.company.model.Dispute

public class Generator {
	
	def static names = ["Emmanuel Christel", "Laure Charline", "Caroline Mayer", "Tim Hubbard", "Arthur Cook", "Daniel Vour"]
	def static comments = ["Prélèvement inattendu.", "Je me suis fait débiter deux fois.", "Prélèvement plus élevé que prévu."]
	
	def static String newName() {
		int index = Math.random() * 6
		names[index]
	}
	
	def static String newComment() {
		int index = Math.random() * 3
		comments[index]
	}
	
	def static int newAmount() {
		Math.round((Math.random() + 1) * 1000)
	}
	
	def static OffsetDateTime newTransactionDate() {
		int days = (Math.random() + 1) * 10
		OffsetDateTime.now().minusDays(days)
	}
	
	def static Dispute createDispute() {
		def dispute = new Dispute()
		
		dispute.customer = newName()
		dispute.description = newComment()
		dispute.amount =  newAmount()
		dispute.transactionDate = newTransactionDate()
		dispute.supervisorApproval = false
		
		dispute
	}
}
