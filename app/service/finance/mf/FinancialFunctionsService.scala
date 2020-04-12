package service.finance.mf

import com.google.inject.ImplementedBy
import service.finance.mf.impl.FinancialFunctionsServiceImpl
import service.models.XirrModels.{XirrRequestDTO, XirrResponseDTO}

@ImplementedBy(classOf[FinancialFunctionsServiceImpl])
trait FinancialFunctionsService {
  def xirr(dto: XirrRequestDTO): Either[Throwable, XirrResponseDTO]
}
