package service.finance.mf.impl

import com.google.inject.Singleton
import com.praphull.finance
import com.praphull.finance.DateRep
import service.finance.mf.FinancialFunctionsService
import service.models.XirrModels.CashFlowType.Sell
import service.models.XirrModels.{XIRRInput, XirrRequestDTO, XirrResponseDTO}

import scala.util.Try

@Singleton
class FinancialFunctionsServiceImpl extends FinancialFunctionsService {
  override def xirr(dto: XirrRequestDTO): Either[Throwable, XirrResponseDTO] = {
    Try {
      require(dto.proposedInvestment.fold(true) { p =>
        p.date.toServiceRep >= dto.currentPosition.date.toServiceRep
      }, "Proposed investment date should be same as or after current position date")

      require(dto.estimatedSellPosition.fold(true) { s =>
        s.date.toServiceRep >= dto.proposedInvestment.fold(dto.currentPosition.date)(_.date).toServiceRep
      }, "Sell position date should be same as or after current position date and proposed investment date")

      val firstInv = dto.firstInvestment
      val investments = {
        val withProposal = dto.proposedInvestment match {
          case Some(investment) => dto.investments :+ investment
          case None => dto.investments
        }
        firstInv :: withProposal
      }
      val (data, totalUnits, totalInvestment) = investments.foldRight((List.empty[XIRRInput], BigDecimal(0), BigDecimal(0))) {
        case (inv, (investmentList, totalUnits, totalInvestment)) =>
          (inv.toServiceRep :: investmentList, totalUnits + inv.units, totalInvestment + inv.value)
      }

      val saleValue = dto.estimatedSellPosition.fold {
        val currentPosition = dto.currentPosition.toServiceRep(totalUnits, Sell)
        dto.proposedInvestment.fold(currentPosition) { investment =>
          //If sell position is not specified but investment proposal is specified, consider sale date as the day after proposed investment
          (DateRep.nextDate(investment.date.toServiceRep), (investment.nav * totalUnits).abs)
        }
      }(_.toServiceRep(totalUnits, Sell))

      val values = data :+ saleValue
      val xirrResult = finance.xirr(values, dto.xirrGuess)
      val returnValue = saleValue._2 - totalInvestment.abs
      XirrResponseDTO(xirrResult, saleValue._1.toString, saleValue._2, totalUnits.abs, totalInvestment, returnValue)
    }.toEither
  }

}
