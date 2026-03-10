## Description

<!-- Brief description of the changes and motivation -->

## Type of Change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Refactor (no functional change; improves code quality or readability)
- [ ] Documentation update
- [ ] Dependency update

## Testing

- [ ] Unit tests added / updated (`./gradlew test`)
- [ ] Integration tests added / updated (`./gradlew integrationTest` — requires Docker)
- [ ] All existing tests pass

## Quality Checklist

- [ ] Code compiles without warnings (`./gradlew build -x test`)
- [ ] Checkstyle passes (`./gradlew checkstyleMain checkstyleTest`)
- [ ] SpotBugs passes (`./gradlew spotbugsMain`)
- [ ] JaCoCo coverage maintained >= 80% (`./gradlew jacocoTestCoverageVerification`)
- [ ] No new `TODO`/`FIXME` comments added

## Spring Boot / Backend Checklist

- [ ] All backend profiles verified (h2, postgres, redis) if application properties changed
- [ ] New `@Service` / `@Repository` beans have integration test coverage
- [ ] No Spring Boot autoconfiguration bypassed without justification
- [ ] Database migrations / schema changes are backward compatible

## Security

- [ ] No secrets, credentials, or private keys committed
- [ ] Input validation applied at API boundary (if applicable)
- [ ] OWASP Top 10 considerations reviewed (if applicable)

## Linked Issues

Closes #

## Notes for Reviewers

<!-- Anything the reviewer should know: design decisions, trade-offs, areas needing extra attention -->