package be.everesst.socialriskdeclaration;

import java.util.List;

/**
 * @param cursor Used for pagination
 */
public record ListResult<T>(List<T> resources, String cursor) {

}
