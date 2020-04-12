package service.models

import com.praphull.finance.DateRep
import play.api.libs.json._

object XirrModels {
  private[service] type XIRRInput = (DateRep, BigDecimal)

  case class UIDateRep(y: Int, m: Int, d: Int) {
    def toServiceRep: DateRep = DateRep(y, m, d)
  }

  object UIDateRep {
    implicit val formats: OFormat[UIDateRep] = Json.format[UIDateRep]
  }

  sealed abstract class CashFlowType(val flow: String) {
    override def toString: String = flow

    private[models] def multiplier: Int
  }

  object CashFlowType {

    case object Buy extends CashFlowType("BUY") {
      override private[models] val multiplier = -1
    }

    case object Sell extends CashFlowType("SELL") {
      override private[models] val multiplier = 1
    }

    private val all = Set(Buy, Sell)

    private def fromStringOpt(s: String): Option[CashFlowType] = all.collectFirst { case c if c.flow == s => c }

    implicit val reads: Reads[CashFlowType] = (json: JsValue) => json.asOpt[String].flatMap(fromStringOpt) match {
      case Some(value) => JsSuccess(value)
      case None => JsError(s"$json is not a valid cash flow type. Allowed values: [${all.mkString(",")}]")
    }
    implicit val writes: Writes[CashFlowType] = (o: CashFlowType) => JsString(o.flow)
  }

  case class CashFlow(date: UIDateRep, amount: BigDecimal, nav: BigDecimal, flowType: CashFlowType) {
    lazy val value: BigDecimal = amount * flowType.multiplier

    def toServiceRep: XIRRInput = date.toServiceRep -> value

    def units: BigDecimal = value / nav
  }

  object CashFlow {
    implicit val formats: OFormat[CashFlow] = Json.format[CashFlow]
  }

  case class Position(date: UIDateRep, nav: BigDecimal) {
    def toServiceRep(units: BigDecimal, cashFlowType: CashFlowType): XIRRInput = {
      (date.toServiceRep, (units * nav).abs * cashFlowType.multiplier)
    }
  }

  object Position {
    implicit val formats: OFormat[Position] = Json.format[Position]
  }

  case class XirrRequestDTO(firstInvestment: CashFlow,
                            investments: List[CashFlow],
                            currentPosition: Position,
                            proposedInvestment: Option[CashFlow],
                            estimatedSellPosition: Option[Position],
                            xirrGuess: Option[BigDecimal])

  object XirrRequestDTO {
    implicit val formats: OFormat[XirrRequestDTO] = Json.format[XirrRequestDTO]
  }

  case class XirrResponseDTO(xirr: BigDecimal, estimatedSaleDate: String, estimatedSaleValue: BigDecimal,
                             absoluteSaleUnits: BigDecimal, totalInvestment: BigDecimal, returnValue: BigDecimal)

  object XirrResponseDTO {
    implicit val writes: OWrites[XirrResponseDTO] = Json.writes[XirrResponseDTO]
  }

  class InvalidCashFlowTypeException(s: String) extends Exception(s"Invalid cash flow type '$s'")

}
