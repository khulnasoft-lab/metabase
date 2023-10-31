import type { CardId, DashboardId, ParameterId } from "metabase-types/api";

// Used to set values for question filters
// Example: "[\"dimension\",[\"field\",17,null]]"
type StringifiedDimension = string;

export interface ClickBehaviorParameterSource {
  id: ParameterId | StringifiedDimension;
  name: string;
  type: "column" | "parameter";
}

export interface ClickBehaviorParameterTarget {
  id: ParameterId | StringifiedDimension;
  type: "parameter" | "dimension";
}

export type ClickBehaviorParameterMapping = Record<
  ParameterId | StringifiedDimension,
  {
    id: ParameterId | StringifiedDimension;
    source: ClickBehaviorParameterSource;
    target: ClickBehaviorParameterTarget;
  }
>;

export type ClickBehaviorType =
  | "action"
  | "actionMenu"
  | "crossfilter"
  | "link";

export type CustomDestinationClickBehaviorEntity = "dashboard" | "question";

export type CustomDestinationClickBehaviorLinkType =
  | CustomDestinationClickBehaviorEntity
  | "url";

export interface CrossFilterClickBehavior {
  type: "crossfilter";
  parameterMapping?: ClickBehaviorParameterMapping;
}

export interface EntityCustomDestinationClickBehavior {
  type: "link";
  linkType: CustomDestinationClickBehaviorEntity;
  targetId: CardId | DashboardId;
  parameterMapping?: ClickBehaviorParameterMapping;
}

export interface ArbitraryCustomDestinationClickBehavior {
  type: "link";
  linkType: "url";
  linkTemplate: string;
  linkTextTemplate?: string;
}

export interface BaseActionClickBehavior {
  type: "action";
  actionType: string;
}

export interface InsertActionClickBehavior extends BaseActionClickBehavior {
  actionType: "insert";
  tableId: number;
}

export interface UpdateActionClickBehavior extends BaseActionClickBehavior {
  actionType: "update";
  objectDetailDashCardId: number;
}

export interface DeleteActionClickBehavior extends BaseActionClickBehavior {
  actionType: "delete";
  objectDetailDashCardId: number;
}

export type ImplicitActionClickBehavior =
  | InsertActionClickBehavior
  | UpdateActionClickBehavior
  | DeleteActionClickBehavior;

/**
 * This is a bit of a hack to allow us using click behavior code
 * for mapping _explicit_ action parameters. We don't actually use the click behavior though.
 * Remove this type and run the type-check to see the errors.
 */
interface HACK_ExplicitActionClickBehavior {
  type: "action";
}

export type ActionClickBehavior =
  | ImplicitActionClickBehavior
  | HACK_ExplicitActionClickBehavior;

/**
 * Makes click handler use default drills.
 * This is virtual, i.e. if a card has no clickBehavior,
 * it'd behave as if it's an "actionMenu".
 */
export type ActionMenuClickBehavior = {
  type: "actionMenu";
};

export type CustomDestinationClickBehavior =
  | EntityCustomDestinationClickBehavior
  | ArbitraryCustomDestinationClickBehavior;

export type ClickBehavior =
  | ActionMenuClickBehavior
  | CrossFilterClickBehavior
  | CustomDestinationClickBehavior
  | ActionClickBehavior;
