import React from "react";
import PropTypes from "prop-types";
import sys from "system-components";

import Icon from "metabase/components/Icon.jsx";
import cx from "classnames";
import _ from "underscore";

const BUTTON_VARIANTS = [
  "small",
  "medium",
  "large",
  "round",
  "primary",
  "danger",
  "warning",
  "cancel",
  "success",
  "purple",
  "white",
  "borderless",
  "onlyIcon",
];

const BaseButton = ({
  className,
  icon,
  iconRight,
  iconSize,
  iconColor,
  iconVertical,
  children,
  ...props
}) => {
  console.log("iconVertical", iconVertical)

  let variantClasses = BUTTON_VARIANTS.filter(variant => props[variant]).map(
    variant => "Button--" + variant,
  );

  const onlyIcon = !children;

  return (
    <button
      {..._.omit(props, ...BUTTON_VARIANTS)}
      className={cx("Button", className, variantClasses)}
    >
      <div
          className={cx("flex layout-centered", {"flex-column": iconVertical })}
          style={iconVertical ? {minWidth: 60} : null }

        >
        {icon && (
          <Icon
            color={iconColor}
            name={icon}
            size={iconSize ? iconSize : 14}
            className={!onlyIcon ? (iconVertical ? "mb1" : "mr1") : null}
          />
        )}
        <div>{children}</div>
        {iconRight && (
          <Icon
            color={iconColor}
            name={iconRight}
            size={iconSize ? iconSize : 14}
            className={!onlyIcon ? (iconVertical ? "mt1" : "ml1") : null}
          />
        )}
      </div>
    </button>
  );
};

BaseButton.propTypes = {
  className: PropTypes.string,
  icon: PropTypes.string,
  iconSize: PropTypes.number,
  children: PropTypes.any,

  small: PropTypes.bool,
  medium: PropTypes.bool,
  large: PropTypes.bool,

  primary: PropTypes.bool,
  warning: PropTypes.bool,
  cancel: PropTypes.bool,
  purple: PropTypes.bool,

  borderless: PropTypes.bool,
};

const Button = sys(
  {
    is: BaseButton,
  },
  "space",
  "color",
);

Button.displayName = "Button";

export default Button;
