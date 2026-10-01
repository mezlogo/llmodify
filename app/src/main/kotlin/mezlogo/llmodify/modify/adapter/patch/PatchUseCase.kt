package mezlogo.llmodify.modify.adapter.patch

import mezlogo.llmodify.modify.model.PatchContext
import mezlogo.llmodify.modify.model.PatchTO

interface PatchUseCase {
  /** Modify, create, move or delete files based on Patch object for given project root. */
  fun patch(patchContext: PatchContext, patchTo: PatchTO)
}
