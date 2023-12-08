import { ExpressionWidget } from "metabase/query_builder/components/expressions/ExpressionWidget";
import * as Lib from "metabase-lib";
import { getUniqueExpressionName } from "metabase-lib/queries/utils/expression";

import type { NotebookStepUiComponentProps } from "../types";
import { ClauseStep } from "./ClauseStep";

export const ExpressionStep = ({
  color,
  updateQuery,
  isLastOpened,
  reportTimezone,
  readOnly,
  step,
}: NotebookStepUiComponentProps): JSX.Element => {
  const { topLevelQuery: query, query: legacyQuery, stageIndex } = step;
  const expressions = Lib.expressions(query, stageIndex);

  const renderExpressionName = (expression: Lib.ExpressionClause) =>
    Lib.displayInfo(query, stageIndex, expression).longDisplayName;

  return (
    <ClauseStep
      color={color}
      items={expressions}
      renderName={renderExpressionName}
      readOnly={readOnly}
      renderPopover={({ item }) => (
        <ExpressionWidget
          legacyQuery={legacyQuery}
          query={query}
          stageIndex={stageIndex}
          name={
            item
              ? Lib.displayInfo(query, stageIndex, item).displayName
              : undefined
          }
          clause={item}
          withName
          onChangeClause={(name, clause) => {
            const expressionsObject = Object.fromEntries(
              expressions.map(expression => [
                Lib.displayInfo(query, stageIndex, expression).displayName,
              ]),
            );
            const uniqueName = getUniqueExpressionName(expressionsObject, name);
            const namedClause = Lib.withExpressionName(clause, uniqueName);
            const isUpdate = item;

            if (isUpdate) {
              const nextQuery = Lib.replaceClause(
                query,
                stageIndex,
                item,
                namedClause,
              );
              updateQuery(nextQuery);
            } else {
              const nextQuery = Lib.expression(
                query,
                stageIndex,
                uniqueName,
                namedClause,
              );
              updateQuery(nextQuery);
            }
          }}
          reportTimezone={reportTimezone}
        />
      )}
      isLastOpened={isLastOpened}
      onRemove={clause => {
        const nextQuery = Lib.removeClause(query, stageIndex, clause);
        updateQuery(nextQuery);
      }}
      withLegacyPopover
    />
  );
};
