package service.finance.mf

import com.praphull.finance.DateRep
import org.scalatest.Assertion
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import service.models.XirrModels.CashFlowType._
import service.models.XirrModels._

class FinancialFunctionsServiceSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  private type XirrResponse = Either[Throwable, XirrResponseDTO]

  private def successful(dto: XirrResponseDTO): XirrResponse = Right(dto)

  private def responseVal[R](res: => XirrResponse,
                             d: XirrResponseDTO => R): R = res match {
    case Left(value) => throw value
    case Right(value) => d(value)
  }

  //private def failure(t: Throwable): XirrResponse = Left(t)
  private val doNotCare = BigDecimal(0)

  private def roundedVal(res: => XirrResponse,
                         d: XirrResponseDTO => BigDecimal, scale: Int): BigDecimal =
    responseVal(res, d).setScale(scale, BigDecimal.RoundingMode.HALF_UP)


  private def responseEquality(res: XirrResponse, expected: XirrResponse): Assertion = {
    roundedVal(res, _.xirr, 10) mustBe roundedVal(expected, _.xirr, 10)
    roundedVal(res, _.estimatedSaleValue, 2) mustBe roundedVal(expected, _.estimatedSaleValue, 2)
    roundedVal(res, _.absoluteSaleUnits, 4) mustBe roundedVal(expected, _.absoluteSaleUnits, 4)
    responseVal(res, _.estimatedSaleDate) mustBe responseVal(expected, _.estimatedSaleDate)
  }

  "XIRR calculation" should {
    "calculate correctly" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])
      val request = XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy)
        ),
        Position(UIDateRep(2020, 4, 1), BigDecimal(13.2116)),
        None, None, None
      )

      val res = financialFunctionsService.xirr(request)
      val expected = successful(XirrResponseDTO(
        xirr = BigDecimal(-0.3574872845),
        estimatedSaleDate = DateRep(2020, 4, 1).toString,
        estimatedSaleValue = BigDecimal(24259.76),
        absoluteSaleUnits = BigDecimal(1836.2468),
        doNotCare, doNotCare
      ))
      responseEquality(res, expected)
    }

    "calculate correctly when only investment proposal is defined" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])
      val request = XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy)
        ),
        Position(date = UIDateRep(2020, 4, 9), nav = BigDecimal(14.3715)),
        Some(CashFlow(date = UIDateRep(2020, 4, 12), amount = BigDecimal(5000), nav = BigDecimal(16.3515), flowType = Buy)),
        None, None
      )

      val res = financialFunctionsService.xirr(request)
      val expected = successful(XirrResponseDTO(
        xirr = BigDecimal(-0.1053554941),
        estimatedSaleDate = DateRep(2020, 4, 13).toString,
        estimatedSaleValue = BigDecimal(35025.39),
        absoluteSaleUnits = BigDecimal(2142.0291),
        doNotCare, doNotCare
      ))
      responseEquality(res, expected)
    }

    "calculate correctly when only selling position is defined" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])
      val request = XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy)
        ),
        Position(date = UIDateRep(2020, 4, 9), nav = BigDecimal(14.3715)),
        None,
        Some(Position(date = UIDateRep(2020, 8, 1), nav = BigDecimal(20.5432))),
        None
      )

      val res = financialFunctionsService.xirr(request)
      val expected = successful(XirrResponseDTO(
        xirr = BigDecimal(0.1564687415),
        estimatedSaleDate = DateRep(2020, 8, 1).toString,
        estimatedSaleValue = BigDecimal(37722.38497688),
        absoluteSaleUnits = BigDecimal(1836.246786),
        doNotCare, doNotCare
      ))
      responseEquality(res, expected)
    }

    "calculate correctly when both investment proposal and sell position are defined" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])
      val request = XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy)
        ),
        Position(date = UIDateRep(2020, 4, 9), nav = BigDecimal(14.3715)),
        Some(CashFlow(date = UIDateRep(2020, 4, 12), amount = BigDecimal(5000), nav = BigDecimal(16.3515), flowType = Buy)),
        Some(Position(date = UIDateRep(2020, 8, 1), nav = BigDecimal(20.5432))),
        None
      )

      val res = financialFunctionsService.xirr(request)
      val expected = successful(XirrResponseDTO(
        xirr = BigDecimal(0.1865593113),
        estimatedSaleDate = DateRep(2020, 8, 1).toString,
        estimatedSaleValue = BigDecimal(44004.13282876),
        absoluteSaleUnits = BigDecimal(2142.029130),
        doNotCare, doNotCare
      ))
      responseEquality(res, expected)
    }

    "not be affected by guessed rate the XIRR calculation outcome" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])
      val request = XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy)
        ),
        Position(date = UIDateRep(2020, 4, 9), nav = BigDecimal(14.3715)),
        Some(CashFlow(date = UIDateRep(2020, 4, 12), amount = BigDecimal(5000), nav = BigDecimal(16.3515), flowType = Buy)),
        Some(Position(date = UIDateRep(2020, 8, 1), nav = BigDecimal(20.5432))),
        Some(BigDecimal(-0.216732))
      )

      val res = financialFunctionsService.xirr(request)
      val expected = successful(XirrResponseDTO(
        xirr = BigDecimal(0.1865593113),
        estimatedSaleDate = DateRep(2020, 8, 1).toString,
        estimatedSaleValue = BigDecimal(44004.13282876),
        absoluteSaleUnits = BigDecimal(2142.029130),
        doNotCare, doNotCare
      ))
      responseEquality(res, expected)
    }

    "allow any order of cash flow dates" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])
      val request = XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy)
        ),
        Position(date = UIDateRep(2020, 4, 9), nav = BigDecimal(14.3715)),
        Some(CashFlow(date = UIDateRep(2020, 4, 12), amount = BigDecimal(5000), nav = BigDecimal(16.3515), flowType = Buy)),
        Some(Position(date = UIDateRep(2020, 8, 1), nav = BigDecimal(20.5432))),
        Some(BigDecimal(-0.216732))
      )

      val res = financialFunctionsService.xirr(request)
      val expected = successful(XirrResponseDTO(
        xirr = BigDecimal(0.1865593113),
        estimatedSaleDate = DateRep(2020, 8, 1).toString,
        estimatedSaleValue = BigDecimal(44004.13282876),
        absoluteSaleUnits = BigDecimal(2142.029130),
        doNotCare, doNotCare
      ))
      responseEquality(res, expected)
    }
    //TODO: Negative cases
  }
}
