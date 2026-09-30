package mezlogo.llmodify.core.configtree

enum class CompareFileStatus {
  FILE_DOES_NOT_EXIST,
  EXPECTED_FILE_IS_A_DIRECTORY,
  FILE_IS_LINK_TO_DECLARED_FILE,
  FILE_IS_LINK_TO_ANOTHER_FILE,
  FILE_IS_REGULAR_FILE,
}
